
package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.BookingStatus;
import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import com.cabride.ride_service.Exception.InvalidRideStatusException;
import com.cabride.ride_service.Repository.RideBookingRepository;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideStopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideStopServiceImplTest {

    @Mock
    private RideStopRepository rideStopRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideBookingRepository rideBookingRepository;

    @InjectMocks
    private RideStopServiceImpl rideStopService;

    private Ride mockRide(UUID rideId) {
        Ride ride = Ride.builder()
                .id(rideId)
                .build();

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        return ride;
    }

    private RideStop createStop(
            UUID rideId,
            UUID bookingId,
            StopType type,
            int sequence
    ) {
        return RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(type)
                .status(StopStatus.PENDING)
                .sequence(sequence)
                .build();
    }

    // CREATE STOPS

    @Test
    void createStops_shouldCreatePickupAndDropStops() {

        UUID rideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        mockRide(rideId);

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .build();

        when(rideBookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(rideStopRepository.findByBookingId(bookingId))
                .thenReturn(List.of());

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of());

        rideStopService.createStops(
                rideId,
                bookingId,
                26.4499,
                80.3319,
                26.4650,
                80.3500
        );

        verify(rideStopRepository).saveAll(argThat(stops -> {
            List<RideStop> savedStops = new ArrayList<>();
            stops.forEach(savedStops::add);

            if (savedStops.size() != 2) {
                return false;
            }

            RideStop pickup = savedStops.get(0);
            RideStop drop = savedStops.get(1);

            return pickup.getStopType() == StopType.PICKUP
                    && drop.getStopType() == StopType.DROP
                    && pickup.getSequence() == 1
                    && drop.getSequence() == 2
                    && pickup.getStatus() == StopStatus.PENDING
                    && drop.getStatus() == StopStatus.PENDING;
        }));
    }

    @Test
    void createStops_shouldRejectBookingFromDifferentRide() {

        UUID rideId = UUID.randomUUID();
        UUID anotherRideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        mockRide(rideId);

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(anotherRideId)
                .build();

        when(rideBookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () ->
                rideStopService.createStops(
                        rideId,
                        bookingId,
                        26.4499,
                        80.3319,
                        26.4650,
                        80.3500
                )
        );

        verify(rideStopRepository, never()).saveAll(any());
    }

    @Test
    void createStops_shouldRejectDuplicateStopsForBooking() {

        UUID rideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        mockRide(rideId);

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .build();

        RideStop existingStop = RideStop.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .build();

        when(rideBookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(rideStopRepository.findByBookingId(bookingId))
                .thenReturn(List.of(existingStop));

        assertThrows(IllegalStateException.class, () ->
                rideStopService.createStops(
                        rideId,
                        bookingId,
                        26.4499,
                        80.3319,
                        26.4650,
                        80.3500
                )
        );

        verify(rideStopRepository, never()).saveAll(any());
    }

    // RETRIEVE STOPS

    @Test
    void getRideStops_shouldReturnStopsInSequenceOrder() {

        UUID rideId = UUID.randomUUID();

        RideStop pickup = createStop(
                rideId, UUID.randomUUID(), StopType.PICKUP, 1
        );

        RideStop drop = createStop(
                rideId, UUID.randomUUID(), StopType.DROP, 2
        );

        when(rideRepository.existsById(rideId))
                .thenReturn(true);

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(pickup, drop));

        List<RideStop> result = rideStopService.getRideStops(rideId);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getSequence());
        assertEquals(2, result.get(1).getSequence());

        verify(rideStopRepository)
                .findByRideIdOrderBySequenceAsc(rideId);
    }

    // UPDATE STOP STATUS

    @Test
    void updateStopStatus_shouldRejectInvalidTransition() {

        UUID stopId = UUID.randomUUID();
        UUID rideId = UUID.randomUUID();

        RideStop stop = RideStop.builder()
                .id(stopId)
                .rideId(rideId)
                .status(StopStatus.COMPLETED)
                .build();

        when(rideStopRepository.findById(stopId))
                .thenReturn(Optional.of(stop));

        mockRide(rideId);

        assertThrows(IllegalStateException.class, () ->
                rideStopService.updateStopStatus(
                        stopId,
                        StopStatus.ARRIVED
                )
        );

        verify(rideStopRepository, never()).save(any(RideStop.class));
    }

    @Test
    void updateStopStatus_shouldUpdatePendingStopToArrived() {

        UUID stopId = UUID.randomUUID();
        UUID rideId = UUID.randomUUID();

        RideStop stop = RideStop.builder()
                .id(stopId)
                .rideId(rideId)
                .status(StopStatus.PENDING)
                .build();

        when(rideStopRepository.findById(stopId))
                .thenReturn(Optional.of(stop));

        mockRide(rideId);

        when(rideStopRepository.save(any(RideStop.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideStop result = rideStopService.updateStopStatus(
                stopId,
                StopStatus.ARRIVED
        );

        assertEquals(StopStatus.ARRIVED, result.getStatus());

        verify(rideStopRepository).save(stop);
    }

    @Test
    void updateStopStatus_shouldCompleteArrivedStop() {

        UUID stopId = UUID.randomUUID();
        UUID rideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        RideStop stop = RideStop.builder()
                .id(stopId)
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .status(StopStatus.ARRIVED)
                .build();

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(rideStopRepository.findById(stopId))
                .thenReturn(Optional.of(stop));

        mockRide(rideId);

        when(rideBookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(rideStopRepository.save(any(RideStop.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideStop result = rideStopService.updateStopStatus(
                stopId,
                StopStatus.COMPLETED
        );

        assertEquals(StopStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());

        verify(rideBookingRepository).save(booking);
        verify(rideStopRepository).save(stop);
    }

    @Test
    void updateStopStatus_shouldNotCompleteBookingWhenPickupCompletes() {

        UUID stopId = UUID.randomUUID();
        UUID rideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        RideStop stop = RideStop.builder()
                .id(stopId)
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .status(StopStatus.ARRIVED)
                .build();

        when(rideStopRepository.findById(stopId))
                .thenReturn(Optional.of(stop));

        mockRide(rideId);

        when(rideStopRepository.save(any(RideStop.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideStop result = rideStopService.updateStopStatus(
                stopId,
                StopStatus.COMPLETED
        );

        assertEquals(StopStatus.COMPLETED, result.getStatus());

        verify(rideBookingRepository, never()).save(any(RideBooking.class));
    }

    @Test
    void updateStopStatus_shouldRejectDropCompletionForNonConfirmedBooking() {

        UUID stopId = UUID.randomUUID();
        UUID rideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        RideStop stop = RideStop.builder()
                .id(stopId)
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .status(StopStatus.ARRIVED)
                .build();

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .status(BookingStatus.CANCELLED)
                .build();

        when(rideStopRepository.findById(stopId))
                .thenReturn(Optional.of(stop));

        mockRide(rideId);

        when(rideBookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThrows(InvalidRideStatusException.class, () ->
                rideStopService.updateStopStatus(
                        stopId,
                        StopStatus.COMPLETED
                )
        );

        verify(rideBookingRepository, never()).save(any(RideBooking.class));
        verify(rideStopRepository, never()).save(any(RideStop.class));
    }
    // REORDER STOPS

    @Test
    void reorderStops_shouldRejectDropBeforePickup() {

        UUID rideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        RideStop pickup = createStop(
                rideId, bookingId, StopType.PICKUP, 1
        );

        RideStop drop = createStop(
                rideId, bookingId, StopType.DROP, 2
        );

        mockRide(rideId);

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(pickup, drop));

        assertThrows(IllegalArgumentException.class, () ->
                rideStopService.reorderStops(
                        rideId,
                        List.of(drop.getId(), pickup.getId())
                )
        );

        verify(rideStopRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void reorderStops_shouldUpdateSequenceSuccessfully() {

        UUID rideId = UUID.randomUUID();
        UUID bookingA = UUID.randomUUID();
        UUID bookingB = UUID.randomUUID();

        RideStop pickupA = createStop(
                rideId, bookingA, StopType.PICKUP, 1
        );

        RideStop dropA = createStop(
                rideId, bookingA, StopType.DROP, 2
        );

        RideStop pickupB = createStop(
                rideId, bookingB, StopType.PICKUP, 3
        );

        RideStop dropB = createStop(
                rideId, bookingB, StopType.DROP, 4
        );

        mockRide(rideId);

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(pickupA, dropA, pickupB, dropB));

        rideStopService.reorderStops(
                rideId,
                List.of(
                        pickupA.getId(),
                        pickupB.getId(),
                        dropA.getId(),
                        dropB.getId()
                )
        );

        assertEquals(1, pickupA.getSequence());
        assertEquals(2, pickupB.getSequence());
        assertEquals(3, dropA.getSequence());
        assertEquals(4, dropB.getSequence());

        verify(rideStopRepository).saveAllAndFlush(any());
        verify(rideStopRepository).saveAll(any());
    }

    @Test
    void reorderStops_shouldRejectIncompleteStopOrder() {

        UUID rideId = UUID.randomUUID();
        UUID bookingA = UUID.randomUUID();
        UUID bookingB = UUID.randomUUID();

        RideStop pickupA = createStop(
                rideId, bookingA, StopType.PICKUP, 1
        );

        RideStop dropA = createStop(
                rideId, bookingA, StopType.DROP, 2
        );

        RideStop pickupB = createStop(
                rideId, bookingB, StopType.PICKUP, 3
        );

        RideStop dropB = createStop(
                rideId, bookingB, StopType.DROP, 4
        );

        mockRide(rideId);

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(pickupA, dropA, pickupB, dropB));

        List<UUID> incompleteOrder = List.of(
                pickupA.getId(),
                dropA.getId(),
                pickupB.getId()
        );

        assertThrows(IllegalArgumentException.class,
                () -> rideStopService.reorderStops(rideId, incompleteOrder));

        verify(rideStopRepository, never()).saveAllAndFlush(any());
        verify(rideStopRepository, never()).saveAll(any());
    }

    @Test
    void reorderStops_shouldRejectDuplicateStopIds() {

        UUID rideId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        RideStop pickup = createStop(
                rideId, bookingId, StopType.PICKUP, 1
        );

        RideStop drop = createStop(
                rideId, bookingId, StopType.DROP, 2
        );

        mockRide(rideId);

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(pickup, drop));

        List<UUID> duplicateOrder = List.of(
                pickup.getId(),
                pickup.getId(),
                drop.getId()
        );

        assertThrows(IllegalArgumentException.class,
                () -> rideStopService.reorderStops(rideId, duplicateOrder));

        verify(rideStopRepository, never()).saveAllAndFlush(any());
        verify(rideStopRepository, never()).saveAll(any());
    }

    @Test
    void reorderStops_shouldRejectNullStopOrder() {

        UUID rideId = UUID.randomUUID();

        mockRide(rideId);

        assertThrows(IllegalArgumentException.class,
                () -> rideStopService.reorderStops(rideId, null));

        verify(rideStopRepository, never())
                .findByRideIdOrderBySequenceAsc(rideId);

        verify(rideStopRepository, never()).saveAllAndFlush(any());
        verify(rideStopRepository, never()).saveAll(any());
    }

    @Test
    void reorderStops_shouldPreserveFixedStopPositions() {

        UUID rideId = UUID.randomUUID();
        UUID bookingA = UUID.randomUUID();
        UUID bookingB = UUID.randomUUID();

        RideStop pickupA = createStop(
                rideId, bookingA, StopType.PICKUP, 1
        );

        RideStop dropA = createStop(
                rideId, bookingA, StopType.DROP, 2
        );

        RideStop pickupB = createStop(
                rideId, bookingB, StopType.PICKUP, 3
        );

        RideStop dropB = createStop(
                rideId, bookingB, StopType.DROP, 4
        );

        dropA.setStatus(StopStatus.COMPLETED);

        mockRide(rideId);

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(pickupA, dropA, pickupB, dropB));

        List<UUID> requestedOrder = List.of(
                pickupA.getId(),
                dropB.getId(),
                pickupB.getId()
        );

        assertThrows(IllegalArgumentException.class,
                () -> rideStopService.reorderStops(rideId, requestedOrder));

        assertEquals(2, dropA.getSequence());

        verify(rideStopRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void reorderStops_shouldAllowReorderingWithinSameSegment() {

        UUID rideId = UUID.randomUUID();
        UUID bookingA = UUID.randomUUID();
        UUID bookingB = UUID.randomUUID();

        RideStop fixedStop = createStop(
                rideId, bookingA, StopType.PICKUP, 1
        );

        fixedStop.setStatus(StopStatus.COMPLETED);

        RideStop dropA = createStop(
                rideId, bookingA, StopType.DROP, 2
        );

        RideStop pickupB = createStop(
                rideId, bookingB, StopType.PICKUP, 3
        );

        RideStop dropB = createStop(
                rideId, bookingB, StopType.DROP, 4
        );

        mockRide(rideId);

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(fixedStop, dropA, pickupB, dropB));

        rideStopService.reorderStops(
                rideId,
                List.of(
                        pickupB.getId(),
                        dropA.getId(),
                        dropB.getId()
                )
        );

        assertEquals(1, fixedStop.getSequence());
        assertEquals(2, pickupB.getSequence());
        assertEquals(3, dropA.getSequence());
        assertEquals(4, dropB.getSequence());

        verify(rideStopRepository).saveAllAndFlush(any());
        verify(rideStopRepository).saveAll(any());
    }
}
