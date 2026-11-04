
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.Coordinate;
import com.cabride.ride_service.Client.RouteResult;
import com.cabride.ride_service.Dto.Request.RouteInsertionRequest;
import com.cabride.ride_service.Dto.Response.RouteInsertionCandidate;
import com.cabride.ride_service.Dto.Response.RouteInsertionResult;
import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import com.cabride.ride_service.Exception.RoutingException;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideStopRepository;
import com.cabride.ride_service.Service.RouteCalculationService;
import com.cabride.ride_service.Service.RouteInsertionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import com.cabride.ride_service.Client.RouteLegResult;
import com.cabride.ride_service.Dto.Response.PassengerDetourResult;
import com.cabride.ride_service.Enums.StopType;


@Service
@RequiredArgsConstructor
public class RouteInsertionServiceImpl implements RouteInsertionService {

    // Maximum additional delay allowed for any existing passenger.
    @Value("${cabride.matching.max-passenger-delay-seconds:300}")
    private double maxPassengerDelaySeconds;

    // Maximum time from the route's starting point to the new passenger's pickup.
    @Value("${cabride.matching.max-pickup-wait-seconds:300}")
    private double maxPickupWaitSeconds;

    // Maximum permitted increase in total route distance.
    @Value("${cabride.matching.max-route-detour-percentage:0.15}")
    private double maxRouteDetourPercentage;

    private final RideRepository rideRepository;
    private final RideStopRepository rideStopRepository;
    private final RouteCalculationService routeCalculationService;

    /**
     * Evaluates possible pickup/drop-off insertions using actual road routes.
     *
     * The candidate with the lowest additional travel duration is selected.
     */
    @Override
    public RouteInsertionResult evaluateInsertion(
            RouteInsertionRequest request
    ) {
        // Validate the incoming request.
        if (request == null
                || request.getRideId() == null
                || request.getBookingId() == null) {
            return infeasible("Ride ID and booking ID are required");
        }

        if (!isValidCoordinate(
                request.getPickupLatitude(),
                request.getPickupLongitude()
        ) || !isValidCoordinate(
                request.getDropLatitude(),
                request.getDropLongitude()
        )) {
            return infeasible("Invalid pickup or drop-off coordinates");
        }

        // Load the ride.
        Ride ride = rideRepository.findById(request.getRideId())
                .orElse(null);

        if (ride == null) {
            return infeasible("Ride not found");
        }

        // This version supports pre-departure route evaluation.
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            return infeasible(
                    "Only ACCEPTED rides can currently be evaluated. "
                            + "Live-location support is required for IN_PROGRESS rides."
            );
        }

        // Retrieve the existing route in its current sequence.
        List<RideStop> existingStops =
                rideStopRepository.findByRideIdOrderBySequenceAsc(
                        request.getRideId()
                );

        if (existingStops == null || existingStops.size() < 2) {
            return infeasible(
                    "At least two existing stops are required"
            );
        }

        // Validate the original route before evaluating alternatives.
        if (!RouteOrderValidator.isPickupBeforeDrop(existingStops)) {
            return infeasible("Existing route has an invalid stop order");
        }

        // Create the proposed passenger's pickup and drop-off stops.
        RideStop newPickup = createStop(
                request.getBookingId(),
                StopType.PICKUP,
                request.getPickupLatitude(),
                request.getPickupLongitude()
        );

        RideStop newDrop = createStop(
                request.getBookingId(),
                StopType.DROP,
                request.getDropLatitude(),
                request.getDropLongitude()
        );

        // Generate all insertion candidates that preserve fixed stops.
        List<RouteInsertionCandidate> candidates =
                generateCandidates(existingStops, newPickup, newDrop);

        if (candidates.isEmpty()) {
            return infeasible("No valid insertion positions were found");
        }

        try {
            // Calculate the existing route's road distance and duration.
            RouteResult originalRoute = routeCalculationService.calculateRoute(
                    toCoordinates(existingStops)
            );

            RouteInsertionCandidate bestCandidate = null;
            double minimumAdditionalDuration = Double.MAX_VALUE;
            double bestAdditionalDistance = 0.0;

            List<PassengerDetourResult> bestPassengerImpacts = List.of();
            double bestPickupWaitSeconds = 0.0;

            // Evaluate each proposed route using OSRM.
            for (RouteInsertionCandidate candidate : candidates) {

                RouteResult proposedRoute =
                        routeCalculationService.calculateRoute(
                                toCoordinates(candidate.getProposedStops())
                        );
                // Calculate the ETA changes experienced by every existing passenger.
                List<PassengerDetourResult> passengerImpacts =
                        calculatePassengerImpacts(
                                existingStops,
                                originalRoute,
                                candidate.getProposedStops(),
                                proposedRoute
                        );

                double additionalDuration =
                        proposedRoute.getDurationSeconds()
                                - originalRoute.getDurationSeconds();

                double additionalDistance =
                        proposedRoute.getDistanceMeters()
                                - originalRoute.getDistanceMeters();

                // Calculate the new passenger's estimated pickup waiting time.
                double pickupWaitSeconds = calculatePickupWaitSeconds(
                        candidate.getPickupIndex(),
                        proposedRoute
                );


// Reject candidates that violate any pooling constraint.
                if (!satisfiesPoolingConstraints(
                        additionalDistance,
                        originalRoute.getDistanceMeters(),
                        pickupWaitSeconds,
                        passengerImpacts
                )) {
                    continue;
                }

                // Ignore routes that do not improve or preserve the baseline.
                // A negative additional duration is valid and represents a
                // shorter route after adding the new passenger's stops.
                if (additionalDuration < minimumAdditionalDuration) {
                    minimumAdditionalDuration = additionalDuration;
                    bestAdditionalDistance = additionalDistance;
                    bestCandidate = candidate;
                    bestPassengerImpacts = passengerImpacts;
                    bestPickupWaitSeconds = pickupWaitSeconds;

                }

            }



            if (bestCandidate == null) {
                return infeasible(
                        "Unable to calculate a valid proposed route"
                );
            }

            return new RouteInsertionResult(
                    true,
                    "A valid route insertion was found",
                    bestAdditionalDistance,
                    minimumAdditionalDuration,
                    bestCandidate.getProposedStops(),
                    bestCandidate.getPickupIndex(),
                    bestCandidate.getDropIndex(),
                    bestPickupWaitSeconds,
                    bestPassengerImpacts
            );
        } catch (RoutingException exception) {
            return infeasible(
                    "Route evaluation failed: " + exception.getMessage()
            );
        }
    }

    /**
     * Generates valid pickup/drop-off insertion candidates.
     * The original route is never modified.
     */
    public List<RouteInsertionCandidate> generateCandidates(
            List<RideStop> existingStops,
            RideStop newPickup,
            RideStop newDrop
    ) {
        if (existingStops == null || newPickup == null || newDrop == null) {
            throw new IllegalArgumentException(
                    "Existing stops, pickup and drop-off cannot be null"
            );
        }

        if (newPickup.getBookingId() == null
                || !newPickup.getBookingId().equals(newDrop.getBookingId())) {
            throw new IllegalArgumentException(
                    "New pickup and drop-off must belong to the same booking"
            );
        }

        if (newPickup.getId() != null
                && newPickup.getId().equals(newDrop.getId())) {
            throw new IllegalArgumentException(
                    "Pickup and drop-off must have different IDs"
            );
        }

        if (newPickup.getStatus() != StopStatus.PENDING
                || newDrop.getStatus() != StopStatus.PENDING) {
            throw new IllegalArgumentException(
                    "New stops must have PENDING status"
            );
        }

        for (RideStop stop : existingStops) {
            if (stop == null || stop.getStatus() == null) {
                throw new IllegalArgumentException(
                        "Existing stops and their statuses cannot be null"
                );
            }
        }

        if (!RouteOrderValidator.isPickupBeforeDrop(existingStops)) {
            return List.of();
        }

        List<StopStatus> stopStatuses = existingStops.stream()
                .map(RideStop::getStatus)
                .toList();

        List<int[]> positions =
                RouteInsertionUtils.generateCrossSegmentInsertionPositions(
                        existingStops.size()
                );

        List<RouteInsertionCandidate> candidates = new ArrayList<>();

        for (int[] position : positions) {
            int pickupIndex = position[0];
            int dropIndex = position[1];

            if (!RouteInsertionUtils.areInsertionsAfterFixedStops(
                    stopStatuses, pickupIndex, dropIndex
            )) {
                continue;
            }

            List<RideStop> proposedStops = new ArrayList<>(existingStops);

            // Insert pickup first; dropIndex refers to the final route.
            proposedStops.add(pickupIndex, newPickup);
            proposedStops.add(dropIndex, newDrop);

            if (RouteOrderValidator.isPickupBeforeDrop(proposedStops)
                    && preservesFixedStops(existingStops, proposedStops)) {

                candidates.add(new RouteInsertionCandidate(
                        proposedStops,
                        pickupIndex,
                        dropIndex
                ));
            }
        }

        return candidates;
    }

    /**
     * Converts ordered ride stops into OSRM coordinates.
     * Coordinate constructor order is longitude, latitude.
     */
    private List<Coordinate> toCoordinates(List<RideStop> stops) {
        return stops.stream()
                .map(stop -> new Coordinate(
                        stop.getLongitude(),
                        stop.getLatitude()
                ))
                .toList();
    }

    /**
     * Creates a pending pickup or drop-off stop for a proposed booking.
     */
    private RideStop createStop(
            UUID bookingId,
            StopType stopType,
            double latitude,
            double longitude
    ) {
        RideStop stop = new RideStop();
        stop.setBookingId(bookingId);
        stop.setStopType(stopType);
        stop.setLatitude(latitude);
        stop.setLongitude(longitude);
        stop.setStatus(StopStatus.PENDING);
        return stop;
    }

    /**
     * Validates latitude and longitude ranges.
     */
    private boolean isValidCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }

    /**
     * Ensures existing fixed stops retain their original order.
     */
    private boolean preservesFixedStops(
            List<RideStop> originalStops,
            List<RideStop> proposedStops
    ) {
        List<RideStop> originalFixedStops = originalStops.stream()
                .filter(stop ->
                        RouteInsertionUtils.isFixedStop(stop.getStatus()))
                .toList();

        List<RideStop> proposedFixedStops = proposedStops.stream()
                .filter(stop ->
                        RouteInsertionUtils.isFixedStop(stop.getStatus()))
                .toList();

        if (originalFixedStops.size() != proposedFixedStops.size()) {
            return false;
        }

        for (int i = 0; i < originalFixedStops.size(); i++) {
            RideStop original = originalFixedStops.get(i);
            RideStop proposed = proposedFixedStops.get(i);

            if (original.getId() != null) {
                if (!original.getId().equals(proposed.getId())) {
                    return false;
                }
            } else if (original != proposed) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a standard infeasible result without a proposed route.
     */
    private RouteInsertionResult infeasible(String reason) {
        return new RouteInsertionResult(
                false,
                reason,
                0.0,
                0.0,
                List.of(),
                -1,
                -1
        );
    }

    public RouteInsertionResult validateStopOrder(
            List<RideStop> proposedStops
    ) {
        boolean valid =
                RouteOrderValidator.isPickupBeforeDrop(proposedStops);

        if (!valid) {
            return new RouteInsertionResult(
                    false,
                    "Invalid route: pickup must precede drop-off for every booking",
                    0.0,
                    0.0,
                    List.of(), -1, -1
            );
        }

        return new RouteInsertionResult(
                true,
                "Stop order is valid",
                0.0,
                0.0,
                List.of(), -1, -1
        );
    }


    /**
     * Calculates the original and proposed pickup/drop-off ETAs for
     * every existing passenger.
     *
     * ETAs are cumulative OSRM leg durations measured from the
     * beginning of the evaluated route.
     */
    private List<PassengerDetourResult> calculatePassengerImpacts(
            List<RideStop> originalStops,
            RouteResult originalRoute,
            List<RideStop> proposedStops,
            RouteResult proposedRoute
    ) {
        Map<String, Double> originalEtas =
                calculateStopEtas(originalStops, originalRoute);

        Map<String, Double> proposedEtas =
                calculateStopEtas(proposedStops, proposedRoute);

        Set<UUID> existingBookingIds = new HashSet<>();

        for (RideStop stop : originalStops) {
            existingBookingIds.add(stop.getBookingId());
        }

        List<PassengerDetourResult> impacts = new ArrayList<>();

        for (UUID bookingId : existingBookingIds) {

            double originalPickup = getEta(
                    originalEtas, bookingId, StopType.PICKUP
            );

            double proposedPickup = getEta(
                    proposedEtas, bookingId, StopType.PICKUP
            );

            double originalDrop = getEta(
                    originalEtas, bookingId, StopType.DROP
            );

            double proposedDrop = getEta(
                    proposedEtas, bookingId, StopType.DROP
            );

            double pickupDelay = proposedPickup - originalPickup;
            double dropDelay = proposedDrop - originalDrop;

            // Negative delay means the passenger arrives earlier.
            double maximumDelay = Math.max(
                    0.0,
                    Math.max(pickupDelay, dropDelay)
            );

            impacts.add(new PassengerDetourResult(
                    bookingId,
                    originalPickup,
                    proposedPickup,
                    pickupDelay,
                    originalDrop,
                    proposedDrop,
                    dropDelay,
                    maximumDelay
            ));
        }

        return impacts;
    }

    /**
     * Calculates cumulative arrival times at each stop using OSRM legs.
     *
     * For N stops, OSRM must return exactly N - 1 legs.
     */
    private Map<String, Double> calculateStopEtas(
            List<RideStop> stops,
            RouteResult route
    ) {
        List<RouteLegResult> legs = route.getLegs();

        if (legs == null || legs.size() != stops.size() - 1) {
            throw new RoutingException(
                    "OSRM did not return valid duration data for every route segment"
            );
        }

        Map<String, Double> etas = new HashMap<>();

        double cumulativeDuration = 0.0;

        // The first stop is the route's starting point.
        for (int i = 0; i < stops.size(); i++) {

            RideStop stop = stops.get(i);

            if (stop.getBookingId() == null || stop.getStopType() == null) {
                throw new RoutingException(
                        "Route contains a stop without booking or stop type"
                );
            }

            String key = createStopKey(
                    stop.getBookingId(), stop.getStopType()
            );

            etas.put(key, cumulativeDuration);

            // Add the next road segment's travel duration.
            if (i < legs.size()) {
                double legDuration = legs.get(i).getDurationSeconds();

                if (!Double.isFinite(legDuration) || legDuration < 0.0) {
                    throw new RoutingException(
                            "OSRM returned an invalid route-leg duration"
                    );
                }

                cumulativeDuration += legDuration;
            }
        }

        return etas;
    }

    /**
     * Retrieves the ETA associated with a particular booking and stop type.
     */
    private double getEta(
            Map<String, Double> etas,
            UUID bookingId,
            StopType stopType
    ) {
        Double eta = etas.get(createStopKey(bookingId, stopType));

        if (eta == null) {
            throw new RoutingException(
                    "Missing ETA for booking " + bookingId
                            + " and stop type " + stopType
            );
        }

        return eta;
    }

    /**
     * Creates a stable key for identifying a booking's pickup or drop-off.
     */
    private String createStopKey(UUID bookingId, StopType stopType) {
        return bookingId + ":" + stopType.name();
    }


    /**
     * Calculates the estimated time required to reach the new passenger's
     * pickup location from the beginning of the evaluated route.
     *
     * The pickup index is its position in the final proposed route.
     */
    private double calculatePickupWaitSeconds(
            int pickupIndex,
            RouteResult proposedRoute
    ) {
        List<RouteLegResult> legs = proposedRoute.getLegs();

        if (pickupIndex < 0 || pickupIndex > legs.size()) {
            throw new RoutingException("Invalid pickup insertion index");
        }

        double pickupWaitSeconds = 0.0;

        // Sum travel durations of all road segments before the pickup.
        for (int i = 0; i < pickupIndex; i++) {
            double duration = legs.get(i).getDurationSeconds();

            if (!Double.isFinite(duration) || duration < 0) {
                throw new RoutingException(
                        "Invalid OSRM duration while calculating pickup ETA"
                );
            }

            pickupWaitSeconds += duration;
        }

        return pickupWaitSeconds;
    }


    /**
     * Validates the proposed route against the pooling constraints.
     */
    private boolean satisfiesPoolingConstraints(
            double additionalDistance,
            double originalDistance,
            double pickupWaitSeconds,
            List<PassengerDetourResult> passengerImpacts
    ) {
        // Reject candidates that exceed the new passenger's pickup wait limit.
        if (pickupWaitSeconds > maxPickupWaitSeconds) {
            return false;
        }

        // Reject candidates that delay any existing passenger excessively.
        boolean exceedsPassengerDelay = passengerImpacts.stream()
                .anyMatch(impact ->
                        impact.getMaximumDelaySeconds()
                                > maxPassengerDelaySeconds
                );

        if (exceedsPassengerDelay) {
            return false;
        }

        // Calculate the relative increase in total route distance.
        double detourPercentage =
                Math.max(0.0, additionalDistance) / originalDistance;

        return detourPercentage <= maxRouteDetourPercentage;
    }



}
