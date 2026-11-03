package com.cabride.ride_service.Service;

import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.StopStatus;

import java.util.List;
import java.util.UUID;

public interface RideStopService {
    void createStops(UUID rideId,UUID bookingId, Double pickupLat, Double pickupLon,
                     Double dropLat, Double dropLon);

    List<RideStop> getRideStops(UUID rideId);
    List<RideStop> getBookingStops(UUID bookingId);

    RideStop updateStopStatus(UUID stopId, StopStatus status);

    void reorderStops(UUID rideId, List<UUID> orderedStopIds);
}
