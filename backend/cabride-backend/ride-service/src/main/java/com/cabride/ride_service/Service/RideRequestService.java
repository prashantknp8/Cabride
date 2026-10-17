package com.cabride.ride_service.Service;

import com.cabride.ride_service.Dto.Request.CreateRideRequest;
import com.cabride.ride_service.Dto.Response.RideRequestResponse;

import java.util.List;
import java.util.UUID;

public interface RideRequestService {

    RideRequestResponse createRequest(
            UUID passengerId,
            CreateRideRequest request
    );


    RideRequestResponse getRequestById(UUID requestId, UUID passengerId);
    RideRequestResponse acceptRequest(
            UUID requestId,
            String token
    );
    RideRequestResponse confirmRequest(
            UUID requestId,
            UUID passengerId,
            String token
    );
    List<RideRequestResponse> getMyRequests(UUID passengerId);
}