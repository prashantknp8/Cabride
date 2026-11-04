
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Dto.Response.RouteInsertionCandidate;
import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import com.cabride.ride_service.Dto.Response.RouteInsertionResult;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideStopRepository;
import com.cabride.ride_service.Service.RouteCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RouteInsertionServiceImplTest {



    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideStopRepository rideStopRepository;

    @Mock
    private RouteCalculationService routeCalculationService;

    @InjectMocks
    private RouteInsertionServiceImpl routeInsertionService;





    @Test
    void validateStopOrder_shouldReturnFeasibleForValidSequence() {
        UUID bookingId = UUID.randomUUID();

        List<RideStop> stops = List.of(
                createStop(bookingId, StopType.PICKUP),
                createStop(bookingId, StopType.DROP)
        );

        RouteInsertionResult result =
                routeInsertionService.validateStopOrder(stops);

        assertTrue(result.isFeasible());
        assertEquals("Stop order is valid", result.getReason());
    }

    @Test
    void validateStopOrder_shouldRejectInvalidPickupDropSequence() {
        UUID bookingId = UUID.randomUUID();

        List<RideStop> stops = List.of(
                createStop(bookingId, StopType.DROP),
                createStop(bookingId, StopType.PICKUP)
        );

        RouteInsertionResult result =
                routeInsertionService.validateStopOrder(stops);

        assertFalse(result.isFeasible());
        assertEquals(
                "Invalid route: pickup must precede drop-off for every booking",
                result.getReason()
        );
    }

    @Test
    void validateStopOrder_shouldRejectEmptyStopList() {

        RouteInsertionResult result =
                routeInsertionService.validateStopOrder(List.of());

        assertFalse(result.isFeasible());
    }

    @Test
    void generateCandidates_shouldGenerateValidInsertionCandidates() {
        UUID existingBookingId = UUID.randomUUID();
        UUID newBookingId = UUID.randomUUID();

        List<RideStop> existingStops = List.of(
                createStop(existingBookingId, StopType.PICKUP),
                createStop(existingBookingId, StopType.DROP)
        );

        RideStop newPickup = createStop(newBookingId, StopType.PICKUP);
        RideStop newDrop = createStop(newBookingId, StopType.DROP);

        List<RouteInsertionCandidate> candidates =
                routeInsertionService.generateCandidates(
                        existingStops, newPickup, newDrop
                );

        assertFalse(candidates.isEmpty());
    }

    @Test
    void generateCandidates_shouldRejectDifferentBookingIds() {
        RideStop newPickup = createStop(UUID.randomUUID(), StopType.PICKUP);
        RideStop newDrop = createStop(UUID.randomUUID(), StopType.DROP);

        assertThrows(
                IllegalArgumentException.class,
                () -> routeInsertionService.generateCandidates(
                        List.of(), newPickup, newDrop
                )
        );
    }

    @Test
    void generateCandidates_shouldRejectNullPickup() {
        RideStop newDrop = createStop(UUID.randomUUID(), StopType.DROP);

        assertThrows(
                IllegalArgumentException.class,
                () -> routeInsertionService.generateCandidates(
                        List.of(), null, newDrop
                )
        );
    }

    @Test
    void generateCandidates_shouldRejectNullDrop() {
        RideStop newPickup = createStop(UUID.randomUUID(), StopType.PICKUP);

        assertThrows(
                IllegalArgumentException.class,
                () -> routeInsertionService.generateCandidates(
                        List.of(), newPickup, null
                )
        );
    }

    @Test
    void generateCandidates_shouldRejectInvalidExistingBookingOrder() {
        UUID existingBookingId = UUID.randomUUID();
        UUID newBookingId = UUID.randomUUID();

        List<RideStop> existingStops = List.of(
                createStop(existingBookingId, StopType.DROP),
                createStop(existingBookingId, StopType.PICKUP)
        );

        RideStop newPickup = createStop(newBookingId, StopType.PICKUP);
        RideStop newDrop = createStop(newBookingId, StopType.DROP);

        List<RouteInsertionCandidate> candidates =
                routeInsertionService.generateCandidates(
                        existingStops, newPickup, newDrop
                );

        assertTrue(candidates.isEmpty());
    }



    @Test
    void generateCandidates_shouldInsertNewStopsAfterLastFixedStop() {
        UUID existingBookingId = UUID.randomUUID();
        UUID newBookingId = UUID.randomUUID();

        // Create existing stops and mark them as fixed.
        RideStop existingPickup = createStop(
                existingBookingId, StopType.PICKUP
        );
        existingPickup.setStatus(StopStatus.COMPLETED);

        RideStop existingDrop = createStop(
                existingBookingId, StopType.DROP
        );
        existingDrop.setStatus(StopStatus.ARRIVED);

        List<RideStop> existingStops = List.of(
                existingPickup, existingDrop
        );

        // Create the new passenger's pending stops.
        RideStop newPickup = createStop(
                newBookingId, StopType.PICKUP
        );
        RideStop newDrop = createStop(
                newBookingId, StopType.DROP
        );

        List<RouteInsertionCandidate> candidates =
                routeInsertionService.generateCandidates(
                        existingStops, newPickup, newDrop
                );

        assertFalse(candidates.isEmpty());

        for (RouteInsertionCandidate candidate : candidates) {
            assertTrue(candidate.getPickupIndex() > 1);
            assertTrue(candidate.getDropIndex() > 1);
        }
    }


    @Test
    void generateCandidates_shouldInsertNewStopsAfterAllFixedStops() {
        UUID existingBookingId = UUID.randomUUID();
        UUID newBookingId = UUID.randomUUID();

        RideStop existingPickup = createStop(
                existingBookingId, StopType.PICKUP
        );
        existingPickup.setStatus(StopStatus.COMPLETED);

        RideStop existingDrop = createStop(
                existingBookingId, StopType.DROP
        );
        existingDrop.setStatus(StopStatus.SKIPPED);

        List<RideStop> existingStops = List.of(
                existingPickup, existingDrop
        );

        RideStop newPickup = createStop(
                newBookingId, StopType.PICKUP
        );
        RideStop newDrop = createStop(
                newBookingId, StopType.DROP
        );

        List<RouteInsertionCandidate> candidates =
                routeInsertionService.generateCandidates(
                        existingStops, newPickup, newDrop
                );

        assertFalse(candidates.isEmpty());

        for (RouteInsertionCandidate candidate : candidates) {
            assertTrue(candidate.getPickupIndex() > 1);
            assertTrue(candidate.getDropIndex() > 1);
        }
    }



    private RideStop createStop(UUID bookingId, StopType stopType) {
        RideStop stop = new RideStop();
        stop.setBookingId(bookingId);
        stop.setStopType(stopType);
        stop.setStatus(StopStatus.PENDING);
        return stop;
    }

}
