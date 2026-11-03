package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Dto.Request.MatchRideRequest;
import com.cabride.ride_service.Dto.Response.MatchRideResponse;
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
import com.cabride.ride_service.Service.GeoUtils;
import com.cabride.ride_service.Service.RideMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RideMatchingServiceImpl implements RideMatchingService {

    // Maximum straight-line distance between existing ride pickup
// and the new passenger's pickup.
    private static final double MAX_PICKUP_DISTANCE_KM = 5.0;
    // Maximum straight-line distance between destination locations.
    private static final double MAX_DESTINATION_DISTANCE_KM = 5.0;
    // Maximum allowed angular difference between two journey directions.
    private static final double MAX_BEARING_DIFFERENCE = 45.0;

    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RideBookingRepository rideBookingRepository;

    @Override
    @Transactional
    public List<MatchRideResponse> findMatchingRides(MatchRideRequest matchRequest) {

        RideRequest rideRequest = rideRequestRepository
                .findById(matchRequest.getRideRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ride Request not found"
                ));

        if (rideRequest.getStatus() != RideRequestStatus.REQUESTED) {
            throw new IllegalStateException(
                    "Only requested rides can be matched"
            );
        }

        List<Ride> candidateRides = rideRepository.findByRideTypeAndStatusInAndAvailableSeatsGreaterThan(
                RideType.SHARED,
                List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS),
                0
        );

        // Only retrieve bookings that can still represent an active commitment.
        List<RideBooking> passengerBookings =
                rideBookingRepository.findByPassengerIdAndStatusIn(
                        rideRequest.getPassengerId(),
                        List.of(
                                BookingStatus.REQUESTED,
                                BookingStatus.DRIVER_ACCEPTED,
                                BookingStatus.PASSENGER_CONFIRMED,
                                BookingStatus.PENDING_APPROVAL,
                                BookingStatus.CONFIRMED
                        )
                );

// Collect ride IDs where the passenger already has an active booking.
        List<UUID> excludedRideIds = passengerBookings.stream()
                .map(RideBooking::getRideId)
                .toList();


        // Remove rides where this passenger already has an active booking.
        List<Ride> eligibleRides = candidateRides.stream()
                .filter(ride -> !excludedRideIds.contains(ride.getId()))
                .toList();

        // Reject rides with invalid pickup or destination coordinates.
        List<Ride> validCoordinateRides = eligibleRides.stream()
                .filter(ride ->
                        GeoUtils.isValidCoordinate(
                                ride.getPickupLatitude(),
                                ride.getPickupLongitude()
                        )
                                && GeoUtils.isValidCoordinate(
                                ride.getDestinationLatitude(),
                                ride.getDestinationLongitude()
                        )
                )
                .toList();

// Validate the new passenger's requested locations.
        if (!GeoUtils.isValidCoordinate(
                rideRequest.getPickupLat(),
                rideRequest.getPickupLon()
        ) || !GeoUtils.isValidCoordinate(
                rideRequest.getDestinationLat(),
                rideRequest.getDestinationLon()
        )) {
            throw new IllegalArgumentException("Invalid ride request coordinates");
        }

        // Keep rides whose pickup locations are within the allowed radius.
        List<Ride> pickupCompatibleRides = validCoordinateRides.stream()
                .filter(ride -> GeoUtils.calculateDistance(
                        ride.getPickupLatitude(),
                        ride.getPickupLongitude(),
                        rideRequest.getPickupLat(),
                        rideRequest.getPickupLon()
                ) <= MAX_PICKUP_DISTANCE_KM)
                .toList();

        // Keep rides whose destinations are within the allowed radius.
        List<Ride> destinationCompatibleRides = pickupCompatibleRides.stream()
                .filter(ride -> GeoUtils.calculateDistance(
                        ride.getDestinationLatitude(),
                        ride.getDestinationLongitude(),
                        rideRequest.getDestinationLat(),
                        rideRequest.getDestinationLon()
                ) <= MAX_DESTINATION_DISTANCE_KM)
                .toList();

        // Keep rides travelling in a direction compatible with the passenger.
        List<Ride> directionCompatibleRides = destinationCompatibleRides.stream()
                .filter(ride -> {

                    double existingRideBearing = GeoUtils.calculateBearing(
                            ride.getPickupLatitude(),
                            ride.getPickupLongitude(),
                            ride.getDestinationLatitude(),
                            ride.getDestinationLongitude()
                    );

                    double passengerBearing = GeoUtils.calculateBearing(
                            rideRequest.getPickupLat(),
                            rideRequest.getPickupLon(),
                            rideRequest.getDestinationLat(),
                            rideRequest.getDestinationLon()
                    );

                    double bearingDifference = GeoUtils.calculateBearingDifference(
                            existingRideBearing,
                            passengerBearing
                    );

                    return bearingDifference <= MAX_BEARING_DIFFERENCE;
                })
                .toList();

        // Keep only rides that have at least one available passenger seat.
        List<Ride> seatCompatibleRides = directionCompatibleRides.stream()
                .filter(ride -> ride.getAvailableSeats() != null)
                .filter(ride -> ride.getAvailableSeats() > 0)
                .toList();

        // Rank compatible rides by their calculated compatibility score.
        List<Ride> rankedRides = seatCompatibleRides.stream()
                .sorted(Comparator.comparingDouble(
                        (Ride ride) -> calculateRideScore(ride, rideRequest)
                ).reversed())
                .toList();

// Convert ranked candidates into response objects.
        return rankedRides.stream()
                .map(this::mapToResponse)
                .toList();

    }

    /**
     * Calculates the compatibility score for an existing ride
     * against a new passenger's ride request.
     */
    private double calculateRideScore(Ride ride, RideRequest rideRequest) {

        // Calculate pickup proximity.
        double pickupDistance = GeoUtils.calculateDistance(
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                rideRequest.getPickupLat(),
                rideRequest.getPickupLon()
        );

        // Calculate destination proximity.
        double destinationDistance = GeoUtils.calculateDistance(
                ride.getDestinationLatitude(),
                ride.getDestinationLongitude(),
                rideRequest.getDestinationLat(),
                rideRequest.getDestinationLon()
        );

        // Calculate journey direction compatibility.
        double existingRideBearing = GeoUtils.calculateBearing(
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getDestinationLatitude(),
                ride.getDestinationLongitude()
        );

        double passengerBearing = GeoUtils.calculateBearing(
                rideRequest.getPickupLat(),
                rideRequest.getPickupLon(),
                rideRequest.getDestinationLat(),
                rideRequest.getDestinationLon()
        );

        double bearingDifference = GeoUtils.calculateBearingDifference(
                existingRideBearing,
                passengerBearing
        );

        return calculateCompatibilityScore(
                pickupDistance,
                destinationDistance,
                bearingDifference
        );
    }

    /**
     * Calculates a compatibility score for a candidate ride.
     *
     * Higher scores indicate better geographic and directional compatibility.
     */
    private double calculateCompatibilityScore(
            double pickupDistance,
            double destinationDistance,
            double bearingDifference) {

        // Normalize each factor to a score between 0 and 1.
        double pickupScore = Math.max(0, Math.min(1,
                1 - (pickupDistance / MAX_PICKUP_DISTANCE_KM)));

        double destinationScore = Math.max(0, Math.min(1,
                1 - (destinationDistance / MAX_DESTINATION_DISTANCE_KM)));

        double directionScore = Math.max(0, Math.min(1,
                1 - (bearingDifference / MAX_BEARING_DIFFERENCE)));

        // Combine the normalized factors using their respective weights.
        return (pickupScore * 40)
                + (destinationScore * 30)
                + (directionScore * 30);
    }

    private MatchRideResponse mapToResponse(Ride ride) {
        return MatchRideResponse.builder()
                .rideId(ride.getId())
                .driverId(ride.getDriverId())
                .rideType(ride.getRideType())
                .rideStatus(ride.getStatus())
                .availableSeats(ride.getAvailableSeats())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .destinationLatitude(ride.getDestinationLatitude())
                .destinationLongitude(ride.getDestinationLongitude())
                .build();
    }
}