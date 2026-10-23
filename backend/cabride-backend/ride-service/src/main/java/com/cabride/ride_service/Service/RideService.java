package com.cabride.ride_service.Service;

import java.util.UUID;

public interface RideService {
    void startRide(UUID rideId, UUID driverId,String token);
    void completeRide(UUID rideId, UUID driverId, String token);
}
