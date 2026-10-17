package com.cabride.ride_service.Dto.Response;

import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.RideType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
public class RideResponse {
    private UUID id;

    private UUID driverId;

    private RideType rideType;

    private RideStatus status;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private Integer availableSeats;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}
