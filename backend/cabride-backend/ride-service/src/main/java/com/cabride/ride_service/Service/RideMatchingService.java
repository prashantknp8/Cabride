package com.cabride.ride_service.Service;

import com.cabride.ride_service.Dto.Request.MatchRideRequest;
import com.cabride.ride_service.Dto.Response.MatchRideResponse;

import java.util.List;

/*
 * Defines operations for discovering suitable rides
 * for incoming passenger requests.
 */
public interface RideMatchingService {
    List<MatchRideResponse> findMatchingRides(MatchRideRequest matchRequest);
}
