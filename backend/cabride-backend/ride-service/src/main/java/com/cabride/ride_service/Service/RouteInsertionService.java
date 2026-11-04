package com.cabride.ride_service.Service;

import com.cabride.ride_service.Dto.Request.RouteInsertionRequest;
import com.cabride.ride_service.Dto.Response.RouteInsertionCandidate;
import com.cabride.ride_service.Dto.Response.RouteInsertionResult;
import com.cabride.ride_service.Entity.RideStop;

import java.util.List;

public interface RouteInsertionService {
    RouteInsertionResult evaluateInsertion(RouteInsertionRequest request);
    public List<RouteInsertionCandidate> generateCandidates(
            List<RideStop> existingStops,
            RideStop newPickup,
            RideStop newDrop
    );
}
