package com.cabride.ride_service.Service.Impl;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Exception.InvalidRideStatusException;
import com.cabride.ride_service.Exception.ResourceNotFoundException;
import com.cabride.ride_service.Repository.RideRepository;
import com.cabride.ride_service.Service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {
    private final RideRepository rideRepository;
    private final DriverClient driverClient;


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
        // Update Driver Service: RESERVED -> ON_TRIP
        driverClient.startTrip(driverId, token);

        ride.setStatus(RideStatus.IN_PROGRESS);
        rideRepository.save(ride);


    }

    @Override
    @Transactional
    public void completeRide(UUID rideId, UUID driverId, String token) {
        Ride ride= rideRepository.findById(rideId)
                .orElseThrow(()->new ResourceNotFoundException(
                        "Ride not found"
                ));

        if(!ride.getDriverId().equals(driverId)){
            throw new InvalidRideStatusException(
                    "You are not authorized to complete this ride"
            );
        }

        if(ride.getStatus()!=RideStatus.IN_PROGRESS){
            throw new InvalidRideStatusException(
                    "Only rides in progress can be completed"
            );
        }
        // change driver status: ON_TRIP -> AVAILABLE
        driverClient.completeTrip(driverId,token);
        // change ride status: IN_PROGRESS->COMPLETED
        ride.setStatus(RideStatus.COMPLETED);
        rideRepository.save(ride);
    }
}
