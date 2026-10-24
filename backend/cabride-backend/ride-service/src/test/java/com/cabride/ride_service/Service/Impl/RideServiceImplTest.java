package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Exception.InvalidRideStatusException;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Service.RideService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @InjectMocks
    private RideServiceImpl rideService;

    @Test
    void startRide_shouldChangeStatusToInProgress(){
        UUID rideId= UUID.randomUUID();
        UUID driverId= UUID.randomUUID();
        String token="test-token";

        Ride ride=Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.ACCEPTED)
                .build();

        when(rideRepository.findById(rideId))
                .thenReturn(Optional.of(ride));

        //ACT
        rideService.startRide(rideId,driverId,token);

        //Assert
        assertEquals(RideStatus.IN_PROGRESS,ride.getStatus());

        verify(driverClient).startTrip(driverId,token);
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
        // Arrange
        UUID rideId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        String token = "test-token";

        Ride ride = Ride.builder()
                .id(rideId)
                .driverId(driverId)
                .status(RideStatus.IN_PROGRESS)
                .build();

        when(rideRepository.findById(rideId))
                .thenReturn(Optional.of(ride));

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

        when(rideRepository.findById(rideId))
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

        when(rideRepository.findById(rideId))
                .thenReturn(Optional.of(ride));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.completeRide(rideId, driverId, token)
        );

        verify(driverClient, never()).completeTrip(any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }


}
