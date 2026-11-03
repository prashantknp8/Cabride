package com.cabride.ride_service.Dto.Response;

import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.RideType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRideResponse {

    private UUID rideId;

    private UUID driverId;

    private RideType rideType;

    private RideStatus rideStatus;

    private Integer availableSeats;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;
}