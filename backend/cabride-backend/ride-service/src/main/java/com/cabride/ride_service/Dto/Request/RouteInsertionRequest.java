package com.cabride.ride_service.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteInsertionRequest {

    private UUID rideId;

    private UUID bookingId;

    private double pickupLatitude;
    private double pickupLongitude;

    private double dropLatitude;
    private double dropLongitude;
}
