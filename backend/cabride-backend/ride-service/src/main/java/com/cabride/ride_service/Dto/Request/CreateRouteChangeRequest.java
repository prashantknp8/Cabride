package com.cabride.ride_service.Dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRouteChangeRequest {

    @NotNull
    private UUID rideId;

    @NotNull
    private UUID bookingId;

    private Double pickupLatitude;
    private Double pickupLongitude;

    private Double dropLatitude;
    private Double dropLongitude;
}