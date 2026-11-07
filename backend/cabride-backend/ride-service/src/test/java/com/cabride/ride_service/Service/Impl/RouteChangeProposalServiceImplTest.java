
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Dto.Request.CreateRouteChangeRequest;
import com.cabride.ride_service.Dto.Response.RouteChangeProposalResponse;
import com.cabride.ride_service.Dto.Response.RouteInsertionResult;
import com.cabride.ride_service.Entity.*;
import com.cabride.ride_service.Enums.*;
import com.cabride.ride_service.Repository.*;
import com.cabride.ride_service.Service.RouteInsertionService;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteChangeProposalServiceImplTest {

    @Mock private RideRepository rideRepository;
    @Mock private RideBookingRepository bookingRepository ;
    @Mock private RideStopRepository rideStopRepository;
    @Mock private RideBookingRepository rideBookingRepository;
    @Mock private RouteChangeProposalRepository proposalRepository;
    @Mock private RouteChangeProposalStopRepository proposalStopRepository;
    @Mock private PassengerConsentRepository consentRepository;
    @Mock private RouteInsertionService routeInsertionService;
    @Mock private RideBooking booking;

    @InjectMocks
    private RouteChangeProposalServiceImpl proposalService;

    private UUID proposalId;
    private UUID rideId;
    private UUID passengerId;
    private UUID bookingId;

    private Ride ride;

    private RouteChangeProposal proposal;
    private PassengerConsent consent;
    private CreateRouteChangeRequest createRouteChangeRequest;

    @BeforeEach
    void setUp() {
        proposalId = UUID.randomUUID();
        rideId = UUID.randomUUID();
        passengerId = UUID.randomUUID();

        ride = mock(Ride.class);
        proposal = mock(RouteChangeProposal.class);
        consent = mock(PassengerConsent.class);

        // Common proposal and ride lookup stubs.
        when(proposal.getRideId()).thenReturn(rideId);
        when(proposal.getStatus())
                .thenReturn(RouteChangeProposalStatus.PENDING);
        when(proposal.getExpiresAt())
                .thenReturn(LocalDateTime.now().plusMinutes(5));

        when(proposalRepository.findById(proposalId))
                .thenReturn(Optional.of(proposal));

        when(proposalRepository.findByIdForUpdate(proposalId))
                .thenReturn(Optional.of(proposal));

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

    }

    @Test
    void submitConsent_shouldRecordApprovalAndWaitForOtherPassengers() {
        when(consent.getStatus())
                .thenReturn(PassengerConsentStatus.PENDING);

        when(consentRepository.countByProposalIdAndStatus(
                proposalId, PassengerConsentStatus.PENDING
        )).thenReturn(1L);
        when(proposal.getBaseRouteVersion()).thenReturn(0L);

        when(consentRepository.findByProposalIdAndPassengerId(
                proposalId, passengerId
        )).thenReturn(Optional.of(consent));

        when(consent.getStatus())
                .thenReturn(PassengerConsentStatus.PENDING);

        proposalService.submitConsent(proposalId, passengerId, true);

        verify(consent).setStatus(PassengerConsentStatus.APPROVED);
        verify(consentRepository).save(consent);

        // Another passenger is still waiting, so activation must not happen.
        verify(rideStopRepository, never())
                .saveAllAndFlush(any());

        verify(rideRepository, never()).save(ride);
    }

    @Test
    void submitConsent_shouldRejectProposalWhenPassengerRejects() {
        when(consentRepository.findByProposalIdAndPassengerId(
                proposalId, passengerId
        )).thenReturn(Optional.of(consent));

        when(consent.getStatus())
                .thenReturn(PassengerConsentStatus.PENDING);

        proposalService.submitConsent(proposalId, passengerId, false);

        verify(consent).setStatus(PassengerConsentStatus.REJECTED);
        verify(consentRepository).save(consent);

        verify(proposal).setStatus(RouteChangeProposalStatus.REJECTED);
        verify(proposalRepository).save(proposal);

        // A rejected proposal must never activate its route.
        verify(rideRepository, never()).save(ride);
    }

    @Test
    void submitConsent_shouldRejectPassengerWhoIsNotRequiredToRespond() {
        when(consentRepository.findByProposalIdAndPassengerId(
                proposalId, passengerId
        )).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> proposalService.submitConsent(
                        proposalId, passengerId, true
                )
        );

        verify(consentRepository, never()).save(any());
    }

    @Test
    void submitConsent_shouldRejectDuplicateResponse() {
        when(consentRepository.findByProposalIdAndPassengerId(
                proposalId, passengerId
        )).thenReturn(Optional.of(consent));

        when(consent.getStatus())
                .thenReturn(PassengerConsentStatus.APPROVED);
        assertThrows(
                IllegalStateException.class,
                () -> proposalService.submitConsent(
                        proposalId, passengerId, true
                )
        );

        verify(consentRepository, never()).save(any());
    }

    @Test
    void submitConsent_shouldExpireProposalWhenDeadlineHasPassed() {
        when(proposal.getExpiresAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        proposalService.submitConsent(proposalId, passengerId, true);

        verify(proposal).setStatus(RouteChangeProposalStatus.EXPIRED);
        verify(proposalRepository).save(proposal);

        verify(consentRepository, never()).save(any());
        verify(rideRepository, never()).save(ride);
    }

    @Test
    void submitConsent_shouldSupersedeProposalWhenRouteVersionChanged() {
        when(ride.getRouteVersion()).thenReturn(2L);

        proposalService.submitConsent(proposalId, passengerId, true);

        verify(proposal).setStatus(RouteChangeProposalStatus.SUPERSEDED);
        verify(proposalRepository).save(proposal);

        verify(consentRepository, never()).save(any());
        verify(rideRepository, never()).save(ride);
    }


    private void stubExistingProposal() {
        when(proposalRepository.findById(proposalId))
                .thenReturn(Optional.of(proposal));
    }

    @Test
    void submitConsent_shouldActivateProposalAfterAllPassengersApprove() {
        when(ride.getId()).thenReturn(rideId);
        when(proposal.getId()).thenReturn(proposalId);
        when(proposal.getBaseRouteVersion()).thenReturn(0L);
        when(proposal.getProposedRouteVersion()).thenReturn(1L);

        when(consentRepository.findByProposalIdAndPassengerId(
                proposalId, passengerId
        )).thenReturn(Optional.of(consent));

        when(consent.getStatus())
                .thenReturn(PassengerConsentStatus.PENDING);

        when(consentRepository.countByProposalIdAndStatus(
                proposalId, PassengerConsentStatus.PENDING
        )).thenReturn(0L);


        UUID existingBookingId = UUID.randomUUID();
        UUID pickupStopId = UUID.randomUUID();
        UUID dropStopId = UUID.randomUUID();

        when(proposalStopRepository.findByProposalIdOrderBySequenceAsc(proposalId))
                .thenReturn(createSnapshots(
                        existingBookingId, pickupStopId, dropStopId
                ));

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(createCurrentStops(
                        existingBookingId, pickupStopId, dropStopId
                ));



        // Add the route snapshot and current-stop repository stubs
        // required by activateProposal(), using matching source stop IDs.

        RouteChangeProposalResponse response =
                proposalService.submitConsent(proposalId, passengerId, true);

        verify(consentRepository).save(consent);
        verify(rideStopRepository).saveAllAndFlush(anyList());
        verify(rideRepository).save(ride);
        verify(proposalRepository).save(proposal);
    }

    @Test
    void submitConsent_shouldNotActivateWhenActiveStopsHaveChanged() {
        UUID existingBookingId = UUID.randomUUID();

        // Snapshot contains the original stop IDs.
        UUID originalPickupId = UUID.randomUUID();
        UUID originalDropId = UUID.randomUUID();

        // Active route now contains different stop IDs.
        UUID changedPickupId = UUID.randomUUID();
        UUID changedDropId = UUID.randomUUID();

        when(ride.getId()).thenReturn(rideId);
        when(proposal.getProposedRouteVersion()).thenReturn(1L);
        when(proposal.getId()).thenReturn(proposalId);

        when(proposalStopRepository.findByProposalIdOrderBySequenceAsc(proposalId))
                .thenReturn(createSnapshots(
                        existingBookingId, originalPickupId, originalDropId
                ));

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(createCurrentStops(
                        existingBookingId, changedPickupId, changedDropId
                ));

        when(proposal.getBaseRouteVersion()).thenReturn(0L);

        when(consentRepository.findByProposalIdAndPassengerId(
                proposalId, passengerId
        )).thenReturn(Optional.of(consent));

        when(consent.getStatus())
                .thenReturn(PassengerConsentStatus.PENDING);

        when(consentRepository.countByProposalIdAndStatus(
                proposalId, PassengerConsentStatus.PENDING
        )).thenReturn(0L);

        // Verify that changed active stops prevent route activation.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> proposalService.submitConsent(
                        proposalId, passengerId, true
                )
        );

        assertEquals(
                "The active route stops changed after proposal creation",
                exception.getMessage()
        );

        verify(consentRepository).save(consent);
        verify(rideRepository, never()).save(any(Ride.class));
    }


    @Disabled
    @Test
    void createProposal_shouldRejectInfeasibleRouteInsertion() {



        // Arrange
        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        when(ride.getId()).thenReturn(rideId);
        when(ride.getStatus()).thenReturn(RideStatus.ACCEPTED);

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(booking.getRideId()).thenReturn(rideId);
        when(booking.getPassengerId()).thenReturn(passengerId);
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);

        when(proposalRepository.findByRideIdAndStatusIn(
                eq(rideId),
                ArgumentMatchers.<RouteChangeProposalStatus>anyList()
        )).thenReturn(Collections.emptyList());

        when(routeInsertionService.evaluateInsertion(any()))
                .thenReturn(new RouteInsertionResult(
                        false,
                        "Route insertion is not feasible",
                        0.0,
                        0.0,
                        Collections.emptyList(),
                        -1,
                        -1
                ));

        // Act and assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proposalService.createProposal(createRouteChangeRequest, passengerId
                )
        );

        // Verify that no proposal was persisted.
        verify(proposalRepository, never()).save(any());
    }
    private List<RideStop> createCurrentStops(
            UUID bookingId,
            UUID pickupStopId,
            UUID dropStopId
    ) {
        RideStop pickup = RideStop.builder()
                .id(pickupStopId)
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .latitude(26.4499)
                .longitude(80.3319)
                .sequence(1)
                .status(StopStatus.PENDING)
                .build();

        RideStop drop = RideStop.builder()
                .id(dropStopId)
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .latitude(26.4650)
                .longitude(80.3500)
                .sequence(2)
                .status(StopStatus.PENDING)
                .build();

        return List.of(pickup, drop);
    }


    private List<RouteChangeProposalStop> createSnapshots(
            UUID bookingId,
            UUID pickupStopId,
            UUID dropStopId
    ) {
        RouteChangeProposalStop pickup =
                RouteChangeProposalStop.builder()
                        .proposalId(proposalId)
                        .sourceStopId(pickupStopId)
                        .bookingId(bookingId)
                        .stopType(StopType.PICKUP)
                        .latitude(26.4499)
                        .longitude(80.3319)
                        .sequence(1)
                        .status(StopStatus.PENDING)
                        .build();

        RouteChangeProposalStop drop =
                RouteChangeProposalStop.builder()
                        .proposalId(proposalId)
                        .sourceStopId(dropStopId)
                        .bookingId(bookingId)
                        .stopType(StopType.DROP)
                        .latitude(26.4650)
                        .longitude(80.3500)
                        .sequence(2)
                        .status(StopStatus.PENDING)
                        .build();

        return List.of(pickup, drop);
    }



}
