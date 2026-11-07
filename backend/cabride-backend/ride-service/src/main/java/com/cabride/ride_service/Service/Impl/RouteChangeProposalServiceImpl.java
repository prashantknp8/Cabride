package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Dto.Request.CreateRouteChangeRequest;
import com.cabride.ride_service.Dto.Response.RouteChangeProposalResponse;
import com.cabride.ride_service.Entity.*;
import com.cabride.ride_service.Enums.*;
import com.cabride.ride_service.Repository.*;
import com.cabride.ride_service.Service.RouteChangeProposalService;
import com.cabride.ride_service.Service.RouteInsertionService;
import com.cabride.ride_service.Dto.Request.RouteInsertionRequest;
import com.cabride.ride_service.Dto.Response.RouteInsertionResult;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteChangeProposalServiceImpl
        implements RouteChangeProposalService {

    private final RideRepository rideRepository;
    private final RideStopRepository rideStopRepository;
    private final RideBookingRepository rideBookingRepository;

    private final RouteChangeProposalRepository proposalRepository;
    private final RouteChangeProposalStopRepository proposalStopRepository;
    private final PassengerConsentRepository consentRepository;

    private final RouteInsertionService routeInsertionService;

    @Value("${cabride.route-consent.expiry-minutes:5}")
    private long consentExpiryMinutes;

    /**
     * Evaluates a proposed insertion and creates a consent proposal.
     *
     * The active route is not modified at this stage.
     */
    @Override
    @Transactional
    public RouteChangeProposalResponse createProposal(
            CreateRouteChangeRequest request,
            UUID requestingPassengerId
    ) {
        if (request == null || requestingPassengerId == null) {
            throw new IllegalArgumentException(
                    "Request and authenticated passenger are required"
            );
        }

        // Lock the ride to prevent two proposals from using the same route version.
        Ride ride = rideRepository.findByIdForUpdate(request.getRideId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Ride not found")
                );

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Route proposals currently support ACCEPTED rides only"
            );
        }

        RideBooking requestingBooking = rideBookingRepository.findById(
                request.getBookingId()
        ).orElseThrow(() ->
                new EntityNotFoundException("Booking not found")
        );

        if (!requestingBooking.getRideId().equals(ride.getId())) {
            throw new IllegalArgumentException(
                    "Booking does not belong to this ride"
            );
        }

        // Prevent one passenger from creating a proposal using another passenger's booking.
        if (!requestingBooking.getPassengerId().equals(requestingPassengerId)) {
            throw new IllegalStateException(
                    "You cannot create a proposal for another passenger's booking"
            );
        }

        validateRequestingBooking(requestingBooking);

        // Supersede any earlier proposal that has not been finalized.
        supersedePendingProposals(ride.getId());

        RouteInsertionRequest insertionRequest = new RouteInsertionRequest();
        insertionRequest.setRideId(request.getRideId());
        insertionRequest.setBookingId(request.getBookingId());
        insertionRequest.setPickupLatitude(request.getPickupLatitude());
        insertionRequest.setPickupLongitude(request.getPickupLongitude());
        insertionRequest.setDropLatitude(request.getDropLatitude());
        insertionRequest.setDropLongitude(request.getDropLongitude());

        // Reuse the existing OSRM route calculation and pooling constraints.
        RouteInsertionResult result =
                routeInsertionService.evaluateInsertion(insertionRequest);

        if (result == null || !result.isFeasible()) {
            throw new IllegalStateException(
                    result == null
                            ? "Route evaluation returned no result"
                            : result.getReason()
            );
        }

        Long currentVersion = ride.getRouteVersion() == null
                ? 0L
                : ride.getRouteVersion();

        RouteChangeProposal proposal = RouteChangeProposal.builder()
                .rideId(ride.getId())
                .requestingBookingId(request.getBookingId())
                .baseRouteVersion(currentVersion)
                .proposedRouteVersion(currentVersion + 1)
                .status(RouteChangeProposalStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(
                        consentExpiryMinutes
                ))
                .build();

        proposal = proposalRepository.save(proposal);

        // Persist a separate snapshot; never modify active stops before approval.
        saveProposedStops(proposal, result.getProposedStops());

        // Only passengers whose arrival times are affected need consent
        // for a pre-departure ACCEPTED ride.
        Set<UUID> affectedBookingIds = result.getPassengerImpacts()
                .stream()
                .filter(impact ->
                        impact.getMaximumDelaySeconds() > 0
                )
                .map(impact -> impact.getBookingId())
                .collect(Collectors.toSet());

        createRequiredConsents(proposal, affectedBookingIds);

        long requiredConsents = consentRepository.countByProposalIdAndStatus(
                proposal.getId(),
                PassengerConsentStatus.PENDING
        );

        // If nobody is affected, the proposal can be activated immediately.
        if (requiredConsents == 0) {
            activateProposal(proposal, ride);
        }

        return toResponse(
                proposal,
                requiredConsents == 0
                        ? "No existing passenger consent was required. Route activated."
                        : "Route proposal created. Waiting for passenger consent."
        );
    }

    /**
     * Records an authenticated passenger's approval or rejection.
     */
    @Override
    @Transactional
    public RouteChangeProposalResponse submitConsent(
            UUID proposalId,
            UUID passengerId,
            boolean approved
    ) {
        // Read the proposal first to identify the ride that must be locked.
        RouteChangeProposal initialProposal =
                proposalRepository.findById(proposalId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Route proposal not found"
                                )
                        );

        // Lock the ride first to serialize route changes for this ride.
        Ride ride = rideRepository.findByIdForUpdate(
                initialProposal.getRideId()
        ).orElseThrow(() ->
                new EntityNotFoundException("Ride not found")
        );

        // Lock the proposal after the ride to maintain consistent lock ordering.
        RouteChangeProposal proposal =
                proposalRepository.findByIdForUpdate(proposalId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Route proposal not found"
                                )
                        );

        // Only pending proposals can receive consent responses.
        if (proposal.getStatus() != RouteChangeProposalStatus.PENDING) {
            if (proposal.getStatus() == RouteChangeProposalStatus.EXPIRED) {
                return toResponse(proposal, "Consent deadline has expired.");
            }

            throw new IllegalStateException(
                    "This proposal is no longer accepting consent"
            );
        }

        // Check the deadline directly before checking the passenger's response.
        // This ensures an expired proposal cannot accept consent, even if the
        // passenger has already responded or the consent record is not pending.
        if (proposal.getExpiresAt() != null
                && !LocalDateTime.now().isBefore(proposal.getExpiresAt())) {

            proposal.setStatus(RouteChangeProposalStatus.EXPIRED);
            proposalRepository.save(proposal);

            return toResponse(
                    proposal,
                    "Consent deadline has expired."
            );
        }

        // A route change invalidates proposals created against an older version.
        long currentRouteVersion =
                ride.getRouteVersion() == null ? 0L : ride.getRouteVersion();

        if (currentRouteVersion != proposal.getBaseRouteVersion()) {
            proposal.setStatus(RouteChangeProposalStatus.SUPERSEDED);
            proposalRepository.save(proposal);

            return toResponse(
                    proposal,
                    "The active route changed. This proposal has been superseded."
            );
        }

        // Only passengers whose consent is required can respond.
        PassengerConsent consent =
                consentRepository.findByProposalIdAndPassengerId(
                        proposalId,
                        passengerId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "You are not required to respond to this proposal"
                        )
                );

        // Prevent a passenger from changing or submitting their response twice.
        if (consent.getStatus() != PassengerConsentStatus.PENDING) {
            throw new IllegalStateException(
                    "You have already responded to this proposal"
            );
        }

        // Record the passenger's decision and response timestamp.
        consent.setStatus(
                approved
                        ? PassengerConsentStatus.APPROVED
                        : PassengerConsentStatus.REJECTED
        );
        consent.setRespondedAt(LocalDateTime.now());

        consentRepository.save(consent);

        // A single rejection is sufficient to reject the entire proposal.
        if (!approved) {
            proposal.setStatus(RouteChangeProposalStatus.REJECTED);
            proposalRepository.save(proposal);

            return toResponse(
                    proposal,
                    "A required passenger rejected the route change."
            );
        }

        // Activate the new route only after every required passenger approves.
        long pendingCount = consentRepository.countByProposalIdAndStatus(
                proposalId,
                PassengerConsentStatus.PENDING
        );

        if (pendingCount == 0) {
            boolean activated = activateProposal(proposal, ride);

            return toResponse(
                    proposal,
                    activated
                            ? "All passengers approved. Route activated."
                            : "Proposal could not be activated because it expired or the route changed."
            );
        }

        return toResponse(
                proposal,
                "Consent recorded. Waiting for remaining passengers."
        );
    }

    /**
     * Returns proposal details.
     */
    @Override
    @Transactional(readOnly = true)
    public RouteChangeProposalResponse getProposal(UUID proposalId) {
        RouteChangeProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Proposal not found")
                );

        return toResponse(proposal, "Route proposal details");
    }

    private void validateRequestingBooking(RideBooking booking) {
        if (booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException(
                    "This booking cannot request a route change"
            );
        }
    }

    /**
     * Replaces older pending proposals so approvals cannot be reused.
     */

    private void saveProposedStops(
            RouteChangeProposal proposal,
            List<RideStop> proposedStops
    ) {
        if (proposedStops == null || proposedStops.isEmpty()) {
            throw new IllegalStateException(
                    "Proposed route does not contain any stops"
            );
        }

        List<RouteChangeProposalStop> snapshots = new ArrayList<>();

        for (int i = 0; i < proposedStops.size(); i++) {
            RideStop stop = proposedStops.get(i);

            snapshots.add(RouteChangeProposalStop.builder()
                    .proposalId(proposal.getId())
                    .sourceStopId(stop.getId())
                    .bookingId(stop.getBookingId())
                    .stopType(stop.getStopType())
                    .latitude(stop.getLatitude())
                    .longitude(stop.getLongitude())
                    .sequence(i + 1)
                    .status(stop.getStatus())
                    .build());
        }

        proposalStopRepository.saveAll(snapshots);
    }

    /**
     * Creates one consent record for each affected, confirmed existing passenger.
     */
    private void createRequiredConsents(
            RouteChangeProposal proposal,
            Set<UUID> affectedBookingIds
    ) {
        for (UUID bookingId : affectedBookingIds) {
            RideBooking booking = rideBookingRepository.findById(bookingId)
                    .orElse(null);

            // Only officially confirmed passengers participate in consent.
            if (booking == null
                    || booking.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }

            // The passenger requesting the new insertion is not an existing passenger.
            if (bookingId.equals(proposal.getRequestingBookingId())) {
                continue;
            }

            PassengerConsent consent = PassengerConsent.builder()
                    .proposalId(proposal.getId())
                    .bookingId(booking.getId())
                    .passengerId(booking.getPassengerId())
                    .status(PassengerConsentStatus.PENDING)
                    .build();

            consentRepository.save(consent);
        }
    }

    /**
     * Activates the stored route snapshot only after all required approvals.
     */
    /**
     * Activates the stored route snapshot only after all required approvals.
     *
     * Existing RideStop records are reused to preserve their database identities.
     * Newly inserted stops are created from the approved snapshot.
     */

    private boolean activateProposal(
            RouteChangeProposal proposal,
            Ride ride
    ) {
        // Only pending proposals can change the active route.
        if (proposal.getStatus() != RouteChangeProposalStatus.PENDING) {
            return false;
        }

        // Prevent activation if the proposal has expired.
        if (proposal.getExpiresAt() == null
                || !proposal.getExpiresAt().isAfter(LocalDateTime.now())) {
            proposal.setStatus(RouteChangeProposalStatus.EXPIRED);
            proposalRepository.save(proposal);
            return false;
        }

        // Compare route versions to detect concurrent route modifications.
        Long activeVersion = ride.getRouteVersion() == null
                ? 0L
                : ride.getRouteVersion();

        if (!Objects.equals(activeVersion, proposal.getBaseRouteVersion())) {
            proposal.setStatus(RouteChangeProposalStatus.SUPERSEDED);
            proposalRepository.save(proposal);
            return false;
        }

        // The next route version must be exactly one greater than the active version.
        if (!Objects.equals(proposal.getProposedRouteVersion(), activeVersion + 1)) {
            throw new IllegalStateException(
                    "Invalid proposed route version"
            );
        }

        // Load the approved route snapshot in its intended stop order.
        List<RouteChangeProposalStop> snapshots =
                proposalStopRepository.findByProposalIdOrderBySequenceAsc(
                        proposal.getId()
                );

        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalStateException(
                    "Proposed route snapshot is missing"
            );
        }

        // Load the currently active route.
        List<RideStop> currentStops =
                rideStopRepository.findByRideIdOrderBySequenceAsc(
                        ride.getId()
                );

        if (currentStops == null) {
            currentStops = new ArrayList<>();
        }

        // Ensure every existing active stop is represented exactly once.
        Set<UUID> snapshotSourceIds = snapshots.stream()
                .map(RouteChangeProposalStop::getSourceStopId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> currentStopIds = currentStops.stream()
                .map(RideStop::getId)
                .collect(Collectors.toSet());

        long nonNullSourceCount = snapshots.stream()
                .filter(snapshot -> snapshot.getSourceStopId() != null)
                .count();

        if (snapshotSourceIds.size() != nonNullSourceCount) {
            throw new IllegalStateException(
                    "Proposed route contains duplicate existing stops"
            );
        }

        if (!snapshotSourceIds.equals(currentStopIds)) {
            throw new IllegalStateException(
                    "The active route stops changed after proposal creation"
            );
        }

        // Ensure proposed sequence numbers are positive and unique.
        Set<Integer> sequences = new HashSet<>();

        for (RouteChangeProposalStop snapshot : snapshots) {
            if (snapshot.getSequence() <= 0
                    || !sequences.add(snapshot.getSequence())) {
                throw new IllegalStateException(
                        "Proposed route contains invalid stop sequences"
                );
            }
        }

        // Validate pickup/drop-off ordering before modifying database records.
        List<RideStop> validationStops = snapshots.stream()
                .map(snapshot -> RideStop.builder()
                        .bookingId(snapshot.getBookingId())
                        .stopType(snapshot.getStopType())
                        .latitude(snapshot.getLatitude())
                        .longitude(snapshot.getLongitude())
                        .sequence(snapshot.getSequence())
                        .status(snapshot.getStatus())
                        .build())
                .toList();

        if (!RouteOrderValidator.isPickupBeforeDrop(validationStops)) {
            throw new IllegalStateException(
                    "Proposed route has an invalid pickup/drop-off order"
            );
        }

        // Index existing stops so their database identities can be preserved.
        Map<UUID, RideStop> existingStops = currentStops.stream()
                .collect(Collectors.toMap(
                        RideStop::getId,
                        Function.identity()
                ));

        // Move existing sequence numbers temporarily to avoid unique-key collisions.
        for (int i = 0; i < currentStops.size(); i++) {
            currentStops.get(i).setSequence(-(i + 1));
        }

        if (!currentStops.isEmpty()) {
            rideStopRepository.saveAllAndFlush(currentStops);
        }

        List<RideStop> activatedStops = new ArrayList<>();

        for (RouteChangeProposalStop snapshot : snapshots) {
            RideStop stop;

            if (snapshot.getSourceStopId() != null) {
                // Reuse an existing stop rather than creating a duplicate.
                stop = existingStops.get(snapshot.getSourceStopId());

                if (stop == null) {
                    throw new IllegalStateException(
                            "An existing route stop could not be found"
                    );
                }
            } else {
                // A null source ID identifies a newly inserted pickup/drop-off.
                stop = RideStop.builder()
                        .rideId(ride.getId())
                        .build();
            }

            // Apply the approved snapshot values to the active stop.
            stop.setRideId(ride.getId());
            stop.setBookingId(snapshot.getBookingId());
            stop.setStopType(snapshot.getStopType());
            stop.setLatitude(snapshot.getLatitude());
            stop.setLongitude(snapshot.getLongitude());
            stop.setSequence(snapshot.getSequence());
            stop.setStatus(snapshot.getStatus());

            activatedStops.add(stop);
        }

        // Persist the final ordered route.
        rideStopRepository.saveAll(activatedStops);

        // Update the route version after the new route has been persisted.
        ride.setRouteVersion(proposal.getProposedRouteVersion());
        rideRepository.save(ride);

        // Mark the proposal as successfully activated.
        proposal.setStatus(RouteChangeProposalStatus.APPROVED);
        proposalRepository.save(proposal);

        return true;
    }


    private void expireIfNecessary(RouteChangeProposal proposal) {
        if (proposal.getStatus() == RouteChangeProposalStatus.PENDING
                && !proposal.getExpiresAt().isAfter(LocalDateTime.now())) {

            proposal.setStatus(RouteChangeProposalStatus.EXPIRED);
            proposalRepository.save(proposal);
        }
    }

    private RouteChangeProposalResponse toResponse(
            RouteChangeProposal proposal,
            String message
    ) {
        return new RouteChangeProposalResponse(
                proposal.getId(),
                proposal.getRideId(),
                proposal.getBaseRouteVersion(),
                proposal.getProposedRouteVersion(),
                proposal.getStatus(),
                proposal.getExpiresAt(),
                message
        );
    }
    /**
     * Marks older pending proposals as superseded when a new route-change
     * proposal is submitted for the same ride.
     *
     * This ensures that passengers cannot approve multiple proposals
     * based on different versions of the same route.
     */
    private void supersedePendingProposals(UUID rideId) {

        // Find all proposals that are still waiting for passenger consent.
        List<RouteChangeProposal> pendingProposals =
                proposalRepository.findByRideIdAndStatusIn(
                        rideId,
                        List.of(RouteChangeProposalStatus.PENDING)
                );

        if (pendingProposals.isEmpty()) {
            return;
        }

        // These proposals are no longer valid because a newer proposal exists.
        for (RouteChangeProposal proposal : pendingProposals) {
            proposal.setStatus(RouteChangeProposalStatus.SUPERSEDED);
        }

        proposalRepository.saveAll(pendingProposals);
    }
}