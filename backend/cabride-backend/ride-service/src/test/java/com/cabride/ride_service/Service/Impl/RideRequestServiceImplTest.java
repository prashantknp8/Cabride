package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Dto.Response.DriverResponse;
import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Entity.RideRequest;
import com.cabride.ride_service.Enums.RideRequestStatus;
import com.cabride.ride_service.Exception.ResourceNotFoundException;
import com.cabride.ride_service.Repository.RideBookingRepository;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RideRequestServiceImplTest {

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideBookingRepository rideBookingRepository;

    @Mock
    private DriverClient driverClient;

    @InjectMocks
    private RideRequestServiceImpl rideRequestService;

    @Test
    void confirmRequest_shouldCreateRideAndBooking(){
        //Arrange
        UUID requestId=UUID.randomUUID();
        UUID passengerId=UUID.randomUUID();
        UUID driverId=UUID.randomUUID();
        String token="test-token";

        RideRequest request=RideRequest.builder()
                .id(requestId)
                .passengerId(passengerId)
                .pickupLat(26.55)
                .pickupLon(80.45)
                .destinationLat(26.50)
                .destinationLon(90.43)
                .status(RideRequestStatus.DRIVER_ACCEPTED)
                .acceptedDriverId(driverId)
                .build();

        DriverResponse driver=new DriverResponse();
        driver.setVehicleCapacity(4);

        when(rideRequestRepository.findByIdAndPassengerIdAndStatus(
                requestId,
                passengerId,
                RideRequestStatus.DRIVER_ACCEPTED
        )).thenReturn(Optional.of(request));

        when(rideBookingRepository.existsByRideRequestId(requestId))
                .thenReturn(false);

        when(driverClient.getDriverById(driverId,token)).thenReturn(driver);

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> {
                    Ride ride=invocation.getArgument(0);
                    ride.setId(UUID.randomUUID());
                    return ride;
                });

        //Act
        rideRequestService.confirmRequest(requestId,passengerId,token);

        //Assert
        verify(rideRepository).save(any(Ride.class));
        verify(rideBookingRepository).save(any(RideBooking.class));
        assertEquals(RideRequestStatus.MATCHED,request.getStatus());
    }
    @Test
    void confirmRequest_shouldRejectInvalidRequest(){
        //Arrange
        UUID requestId=UUID.randomUUID();
        UUID passengerId=UUID.randomUUID();
        UUID driverId=UUID.randomUUID();
        String token="test-token";

        when(rideRequestRepository.findByIdAndPassengerIdAndStatus(
                requestId,
                passengerId,
                RideRequestStatus.DRIVER_ACCEPTED
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                ()->rideRequestService.confirmRequest(
                        requestId,passengerId,token
                )
        );
        verify(rideRepository,never()).save(any(Ride.class));
        verify(rideBookingRepository, never()).save(any(RideBooking.class));

    }

    @Test
    void confirmRequest_shouldRejectInvalidVehicleCapacity() {
        UUID requestId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        String token = "test-token";

        RideRequest request = RideRequest.builder()
                .id(requestId)
                .passengerId(passengerId)
                .status(RideRequestStatus.DRIVER_ACCEPTED)
                .acceptedDriverId(driverId)
                .build();

        DriverResponse driver = new DriverResponse();
        driver.setVehicleCapacity(0);

        when(rideRequestRepository
                .findByIdAndPassengerIdAndStatus(
                        requestId,
                        passengerId,
                        RideRequestStatus.DRIVER_ACCEPTED))
                .thenReturn(Optional.of(request));

        when(rideBookingRepository.existsByRideRequestId(requestId))
                .thenReturn(false);

        when(driverClient.getDriverById(driverId, token))
                .thenReturn(driver);

        assertThrows(
                IllegalStateException.class,
                () -> rideRequestService.confirmRequest(
                        requestId, passengerId, token)
        );

        verify(rideRepository, never()).save(any(Ride.class));
        verify(rideBookingRepository, never()).save(any(RideBooking.class));
    }



}
