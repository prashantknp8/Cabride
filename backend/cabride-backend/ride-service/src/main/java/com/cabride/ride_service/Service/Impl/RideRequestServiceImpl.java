package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Dto.Request.CreateRideRequest;
import com.cabride.ride_service.Dto.Response.DriverResponse;
import com.cabride.ride_service.Dto.Response.RideRequestResponse;
import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Entity.RideRequest;
import com.cabride.ride_service.Enums.BookingStatus;
import com.cabride.ride_service.Enums.RideRequestStatus;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.RideType;
import com.cabride.ride_service.Exception.ResourceNotFoundException;
import com.cabride.ride_service.Repository.RideBookingRepository;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideRequestRepository;
import com.cabride.ride_service.Service.RideRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RideRequestServiceImpl implements RideRequestService {

    private final RideRequestRepository rideRequestRepository;
    private final DriverClient driverClient;
    private final RideRepository rideRepository;
    private final RideBookingRepository rideBookingRepository;

    @Override
    @Transactional
    public RideRequestResponse createRequest(
            UUID passengerId,
            CreateRideRequest request
    ) {
        RideRequest rideRequest = RideRequest.builder()
                .passengerId(passengerId)
                .pickupLat(request.getPickupLat())
                .pickupLon(request.getPickupLon())
                .destinationLat(request.getDestinationLat())
                .destinationLon(request.getDestinationLon())
                .status(RideRequestStatus.REQUESTED)
                .build();

        RideRequest savedRequest = rideRequestRepository.save(rideRequest);

        return mapToResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public RideRequestResponse getRequestById(
            UUID requestId,
            UUID passengerId
    ) {
        RideRequest rideRequest =
                rideRequestRepository
                        .findByIdAndPassengerId(requestId, passengerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Ride request not found"
                                ));

        return mapToResponse(rideRequest);
    }



    @Override
    @Transactional
    public RideRequestResponse acceptRequest(UUID requestId, String token) {
        RideRequest rideRequest = rideRequestRepository
                .findByIdAndStatus(
                        requestId,
                        RideRequestStatus.REQUESTED
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ride request not found or already processed"
                ));

        DriverResponse driver = driverClient.getMyDriverProfile(token);

        if (!Objects.equals(driver.getStatus(), "AVAILABLE")) {
            throw new IllegalStateException(
                    "Driver must be available to accept a ride request"
            );
        }

        // Reserve the driver before assigning the request
        driverClient.reserveDriver(driver.getId(), token);

        rideRequest.setAcceptedDriverId(driver.getId());
        rideRequest.setStatus(RideRequestStatus.DRIVER_ACCEPTED);

        RideRequest savedRequest = rideRequestRepository.save(rideRequest);

        return mapToResponse(savedRequest);
    }


    @Override
    @Transactional
    public RideRequestResponse confirmRequest(
            UUID requestId,
            UUID passengerId,
            String token
    ) {
        RideRequest request = rideRequestRepository
                .findByIdAndPassengerIdAndStatus(
                        requestId,
                        passengerId,
                        RideRequestStatus.DRIVER_ACCEPTED
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request not found or not awaiting confirmation"
                ));

        if(rideBookingRepository.existsByRideRequestId(request.getId())){
            throw new IllegalStateException(
                    "This ride request has already been confirmed"
            );
        }

        DriverResponse driver = driverClient.getDriverById(
                request.getAcceptedDriverId(),
                token
        );

        if (driver.getVehicleCapacity() == null
                || driver.getVehicleCapacity() < 1) {
            throw new IllegalStateException(
                    "Driver vehicle capacity is missing or invalid"
            );
        }

        int availableSeats = driver.getVehicleCapacity() - 1;

        Ride ride = Ride.builder()
                .driverId(request.getAcceptedDriverId())
                .rideType(RideType.SOLO)
                .status(RideStatus.ACCEPTED)
                .pickupLatitude(request.getPickupLat())
                .pickupLongitude(request.getPickupLon())
                .destinationLatitude(request.getDestinationLat())
                .destinationLongitude(request.getDestinationLon())
                .availableSeats(availableSeats)
                .build();

        Ride savedRide = rideRepository.save(ride);

        RideBooking booking = RideBooking.builder()
                .rideId(savedRide.getId())
                .rideRequestId(request.getId())
                .passengerId(passengerId)
                .pickupLat(request.getPickupLat())
                .pickupLon(request.getPickupLon())
                .dropLat(request.getDestinationLat())
                .dropLon(request.getDestinationLon())
                .status(BookingStatus.CONFIRMED)
                .build();

        rideBookingRepository.save(booking);

        request.setStatus(RideRequestStatus.MATCHED);
        rideRequestRepository.save(request);

        return mapToResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideRequestResponse> getMyRequests(UUID passengerId) {
        return rideRequestRepository.findByPassengerId(passengerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private RideRequestResponse mapToResponse(RideRequest request) {
        return RideRequestResponse.builder()
                .id(request.getId())
                .passengerId(request.getPassengerId())
                .pickupLat(request.getPickupLat())
                .pickupLon(request.getPickupLon())
                .destinationLat(request.getDestinationLat())
                .destinationLon(request.getDestinationLon())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}