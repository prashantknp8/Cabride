package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Entity.*;
import com.cabride.ride_service.Enums.*;
import com.cabride.ride_service.Enums.StopType;
import com.cabride.ride_service.Exception.InvalidRideStatusException;
import com.cabride.ride_service.Exception.ResourceNotFoundException;
import com.cabride.ride_service.Repository.RideBookingRepository;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Repository.RideStopRepository;
import com.cabride.ride_service.Service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {
    private final RideRepository rideRepository;
    private final DriverClient driverClient;
    private final RideStopRepository rideStopRepository;
    private final RideBookingRepository rideBookingRepository;


    @Override
    @Transactional
    public void startRide(UUID rideId, UUID driverId,String token) {
        Ride ride=rideRepository.findById(rideId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Ride not found"
                ));

        if(!ride.getDriverId().equals(driverId)){
            throw new InvalidRideStatusException("You are not authorized to start this ride");
        }
        if(ride.getStatus()!= RideStatus.ACCEPTED){
            throw new InvalidRideStatusException(
                    "Only accepted rides can be started"
            );
        }
        validateRideStops(rideId);

        // Update Driver Service: RESERVED -> ON_TRIP
        driverClient.startTrip(driverId, token);

        ride.setStatus(RideStatus.IN_PROGRESS);
        rideRepository.save(ride);


    }

    @Override
    @Transactional
    public void completeRide(UUID rideId, UUID driverId, String token) {

        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ride not found"
                ));

        if (!driverId.equals(ride.getDriverId())) {
            throw new InvalidRideStatusException(
                    "You are not authorized to complete this ride"
            );
        }

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStatusException(
                    "Only rides in progress can be completed"
            );
        }

        List<RideBooking> bookings =
                rideBookingRepository.findByRideId(rideId);

        if (bookings.isEmpty()) {
            throw new InvalidRideStatusException(
                    "Ride cannot be completed without bookings"
            );
        }

        for (RideBooking booking : bookings) {

            // Cancelled bookings do not block ride completion.
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                continue;
            }

            if (booking.getStatus() != BookingStatus.COMPLETED) {
                throw new InvalidRideStatusException(
                        "All active passenger journeys must be completed"
                );
            }

            List<RideStop> stops =
                    rideStopRepository.findByBookingId(booking.getId());

            if (stops.size() != 2) {
                throw new InvalidRideStatusException(
                        "Every active booking must have exactly two stops"
                );
            }

            boolean pickupCompleted = false;
            boolean dropCompleted = false;

            for (RideStop stop : stops) {

                if (stop.getStatus() != StopStatus.COMPLETED) {
                    throw new InvalidRideStatusException(
                            "All passenger stops must be completed"
                    );
                }

                if (stop.getStopType() == StopType.PICKUP) {
                    if (pickupCompleted) {
                        throw new InvalidRideStatusException(
                                "Duplicate pickup stop found"
                        );
                    }
                    pickupCompleted = true;

                } else if (stop.getStopType() == StopType.DROP) {
                    if (dropCompleted) {
                        throw new InvalidRideStatusException(
                                "Duplicate drop stop found"
                        );
                    }
                    dropCompleted = true;
                }
            }

            if (!pickupCompleted || !dropCompleted) {
                throw new InvalidRideStatusException(
                        "Every active booking must have a completed pickup and drop"
                );
            }
        }

        // Update Driver Service: ON_TRIP -> AVAILABLE
        driverClient.completeTrip(driverId, token);

        // Update Ride: IN_PROGRESS -> COMPLETED
        ride.setStatus(RideStatus.COMPLETED);
        rideRepository.save(ride);
    }

    
    private void validateRideStops(UUID rideId) {
        List<RideStop> stops =
                rideStopRepository.findByRideIdOrderBySequenceAsc(rideId);

        if (stops.isEmpty()) {
            throw new InvalidRideStatusException(
                    "RIde cannot be started without pickup and drop stops"
            );
        }

        List<RideBooking> bookings = rideBookingRepository.findByRideId(rideId);

        if (bookings.isEmpty()) {
            throw new InvalidRideStatusException(
                    "Ride cannot be started without confirmed booking"
            );
        }

        Map<UUID, Integer> pickupPositions = new HashMap<>();
        Map<UUID, Integer> dropPositions = new HashMap<>();

        for (int i = 0; i < stops.size(); i++) {

            RideStop stop = stops.get(i);

            if (stop.getStatus() != StopStatus.PENDING) {
                throw new InvalidRideStatusException(
                        "All stops must be pending before ride starts"
                );
            }

            if (stop.getStopType() == StopType.PICKUP) {
                if (pickupPositions.put(stop.getBookingId(), i) != null) {
                    throw new InvalidRideStatusException(
                            "Duplicate pickup stop found"
                    );
                }
            } else if (stop.getStopType() == StopType.DROP) {
                if (dropPositions.put(stop.getBookingId(), i) != null) {
                    throw new InvalidRideStatusException(
                            "Duplicate drop stop found"
                    );
                }
            }
        }

        for (RideBooking booking : bookings) {

            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                throw new InvalidRideStatusException(
                        "All bookings must be confirmed before the ride starts"
                );
            }

            UUID bookingId = booking.getId();

            Integer pickupPosition = pickupPositions.get(bookingId);
            Integer dropPosition = dropPositions.get(bookingId);

            if (pickupPosition == null || dropPosition == null) {
                throw new InvalidRideStatusException(
                        "Every booking must have one pickup and one drop stop"
                );
            }

            if (pickupPosition >= dropPosition) {
                throw new InvalidRideStatusException(
                        "Pickup must appear before drop"
                );
            }
        }
    }
}