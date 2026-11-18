package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Dto.Request.CreateRideRequest;
import com.cabride.ride_service.Dto.Request.CreateRouteChangeRequest;
import com.cabride.ride_service.Dto.Response.DriverResponse;
import com.cabride.ride_service.Dto.Response.RideRequestResponse;
import com.cabride.ride_service.Dto.Response.RouteChangeProposalResponse;
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
import com.cabride.ride_service.Service.RouteChangeProposalService;
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
    private final RouteChangeProposalService routeChangeProposalService;
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
    @Transactional
    public RouteChangeProposalResponse bookSharedRide(
            UUID requestId,
            UUID rideId,
            UUID passengerId
    ) {
        // Lock the ride first so concurrent passengers cannot reserve the same seat.
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shared ride not found"
                ));

        // Lock the passenger request to prevent duplicate booking attempts.
        RideRequest request = rideRequestRepository
                .findByIdAndPassengerIdAndStatus(
                        requestId,
                        passengerId,
                        RideRequestStatus.REQUESTED
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ride request not found or already processed"
                ));

        if (rideBookingRepository.existsByRideRequestId(requestId)) {
            throw new IllegalStateException(
                    "This ride request already has a booking"
            );
        }

        if (ride.getRideType() != RideType.SHARED
                || ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Selected ride is not an available shared ride"
            );
        }

        if (ride.getAvailableSeats() == null
                || ride.getAvailableSeats() <= 0) {
            throw new IllegalStateException(
                    "No seats available in this ride"
            );
        }

        // Reserve the seat immediately, including while consent is pending.
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        rideRepository.save(ride);

        RideBooking booking = RideBooking.builder()
                .rideId(ride.getId())
                .rideRequestId(request.getId())
                .passengerId(passengerId)
                .pickupLat(request.getPickupLat())
                .pickupLon(request.getPickupLon())
                .dropLat(request.getDestinationLat())
                .dropLon(request.getDestinationLon())
                .status(BookingStatus.PENDING_APPROVAL)
                .build();

        RideBooking savedBooking = rideBookingRepository.save(booking);

        CreateRouteChangeRequest proposalRequest =
                new CreateRouteChangeRequest();

        proposalRequest.setRideId(ride.getId());
        proposalRequest.setBookingId(savedBooking.getId());
        proposalRequest.setPickupLatitude(request.getPickupLat());
        proposalRequest.setPickupLongitude(request.getPickupLon());
        proposalRequest.setDropLatitude(request.getDestinationLat());
        proposalRequest.setDropLongitude(request.getDestinationLon());

        // If proposal creation fails, the transaction rolls back the seat
        // reservation and booking together.
        return routeChangeProposalService.createProposal(
                proposalRequest,
                passengerId
        );
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