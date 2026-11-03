package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.BookingStatus;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import com.cabride.ride_service.Exception.InvalidRideStatusException;
import com.cabride.ride_service.Repository.RideBookingRepository;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideStopRepository;
import com.cabride.ride_service.Service.RideService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {
    @Mock
    private RideRepository rideRepository;
    @Mock
    private DriverClient driverClient;
    @Mock
    private RideStopRepository rideStopRepository;

    @Mock
    private RideBookingRepository rideBookingRepository;
    @InjectMocks
    private RideServiceImpl rideService;

    @Test
    void startRide_shouldChangeStatusToInProgress() {

        UUID rideId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.ACCEPTED)
                .build();

        when(rideRepository.findById(rideId))
                .thenReturn(Optional.of(ride));

        // Arrange: Create valid pickup and drop stops
        RideStop pickup = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .sequence(1)
                .status(StopStatus.PENDING)
                .build();

        RideStop drop = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .sequence(2)
                .status(StopStatus.PENDING)
                .build();

        when(rideStopRepository.findByRideIdOrderBySequenceAsc(rideId))
                .thenReturn(List.of(pickup, drop));

        // Arrange: Create confirmed booking
        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(rideBookingRepository.findByRideId(rideId))
                .thenReturn(List.of(booking));

        // Act
        rideService.startRide(rideId, driverId, token);

        // Assert
        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());

        verify(driverClient).startTrip(driverId, token);
        verify(rideRepository).save(ride);
    }
    @Test
    void startRide_shouldRejectDifferentDriver(){
        UUID rideId=UUID.randomUUID();
        UUID assignedDriver=UUID.randomUUID();
        UUID anotherDriver=UUID.randomUUID();
        String token="test-token";

        Ride ride =Ride.builder()
                .id(rideId)
                .driverId(assignedDriver)
                .status(RideStatus.ACCEPTED)
                .build();

        when(rideRepository.findById(rideId))
                .thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStatusException.class,
                ()-> rideService.startRide(rideId,anotherDriver,token));
    }

    @Test
    void completeRide_shouldMarkRideCompletedAndReleaseDriver() {

        UUID rideId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.IN_PROGRESS)
                .build();

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .status(BookingStatus.COMPLETED)
                .build();

        when(rideBookingRepository.findByRideId(rideId))
                .thenReturn(List.of(booking));

        RideStop pickup = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .status(StopStatus.COMPLETED)
                .sequence(1)
                .build();

        RideStop drop = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .status(StopStatus.COMPLETED)
                .sequence(2)
                .build();

        when(rideStopRepository.findByBookingId(bookingId))
                .thenReturn(List.of(pickup, drop));

        // Act
        rideService.completeRide(rideId, driverId, token);

        // Assert
        assertEquals(RideStatus.COMPLETED, ride.getStatus());

        verify(driverClient).completeTrip(driverId, token);
        verify(rideRepository).save(ride);
    }
    @Test
    void completeRide_shouldRejectDifferentDriver() {
        UUID rideId = UUID.randomUUID();
        UUID assignedDriverId = UUID.randomUUID();
        UUID anotherDriverId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(assignedDriverId)
                .status(RideStatus.IN_PROGRESS)
                .build();

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.completeRide(
                        rideId, anotherDriverId, token)
        );

        verify(driverClient, never()).completeTrip(any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }
    @Test
    void completeRide_shouldRejectRideNotInProgress() {
        UUID rideId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.ACCEPTED)
                .build();

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.completeRide(rideId, driverId, token)
        );

        verify(driverClient, never()).completeTrip(any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void completeRide_shouldRejectConfirmedBooking() {

        UUID rideId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.IN_PROGRESS)
                .build();

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        when(rideBookingRepository.findByRideId(rideId))
                .thenReturn(List.of(booking));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.completeRide(rideId, driverId, token)
        );

        verify(driverClient, never()).completeTrip(any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void completeRide_shouldRejectUnfinishedPassengerStops() {

        UUID rideId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.IN_PROGRESS)
                .build();

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .status(BookingStatus.COMPLETED)
                .build();

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        when(rideBookingRepository.findByRideId(rideId))
                .thenReturn(List.of(booking));

        RideStop pickup = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .status(StopStatus.PENDING)
                .sequence(1)
                .build();

        RideStop drop = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .status(StopStatus.COMPLETED)
                .sequence(2)
                .build();

        when(rideStopRepository.findByBookingId(bookingId))
                .thenReturn(List.of(pickup, drop));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.completeRide(rideId, driverId, token)
        );

        verify(driverClient, never()).completeTrip(any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void completeRide_shouldRejectMissingOrDuplicateStops() {

        UUID rideId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.IN_PROGRESS)
                .build();

        RideBooking booking = RideBooking.builder()
                .id(bookingId)
                .rideId(rideId)
                .status(BookingStatus.COMPLETED)
                .build();

        when(rideRepository.findByIdForUpdate(rideId))
                .thenReturn(Optional.of(ride));

        when(rideBookingRepository.findByRideId(rideId))
                .thenReturn(List.of(booking));

        // Case 1: Missing DROP stop
        RideStop pickup = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .status(StopStatus.COMPLETED)
                .sequence(1)
                .build();

        when(rideStopRepository.findByBookingId(bookingId))
                .thenReturn(List.of(pickup));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.completeRide(rideId, driverId, token)
        );

        verify(driverClient, never()).completeTrip(any(), any());
        verify(rideRepository, never()).save(any(Ride.class));

        // Case 2: Duplicate PICKUP stops
        RideStop duplicatePickup = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.PICKUP)
                .status(StopStatus.COMPLETED)
                .sequence(2)
                .build();

        RideStop drop = RideStop.builder()
                .id(UUID.randomUUID())
                .rideId(rideId)
                .bookingId(bookingId)
                .stopType(StopType.DROP)
                .status(StopStatus.COMPLETED)
                .sequence(3)
                .build();

        when(rideStopRepository.findByBookingId(bookingId))
                .thenReturn(List.of(pickup, duplicatePickup, drop));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.completeRide(rideId, driverId, token)
        );

        verify(driverClient, never()).completeTrip(any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }



}
