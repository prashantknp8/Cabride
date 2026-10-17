package com.cabride.ride_service.Dto.Response;


import com.cabride.ride_service.Enums.RideRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class RideRequestResponse {

    private UUID id;
    private UUID passengerId;

    private Double pickupLat;
    private Double pickupLon;

    private Double destinationLat;
    private Double destinationLon;

    private RideRequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
