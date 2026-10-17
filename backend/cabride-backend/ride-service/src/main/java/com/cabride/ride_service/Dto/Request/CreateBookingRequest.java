package com.cabride.ride_service.Dto.Request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Ride Id is required")
    private UUID rideId;

    @NotNull(message ="Pickup latitude is required")
    @DecimalMin(value= "-99.0")
    @DecimalMax(value = "99.0")
    private Double pickupLat;

    @NotNull(message ="Pickup longitude is required")
    @DecimalMin(value= "-180.0")
    @DecimalMax(value = "180.0")
    private Double pickupLon;

    @NotNull(message ="Drop latitude is required")
    @DecimalMin(value= "-99.0")
    @DecimalMax(value = "99.0")
    private Double dropLat;

    @NotNull(message ="Drop longitude is required")
    @DecimalMin(value= "-180.0")
    @DecimalMax(value = "180.0")
    private Double dropLon;
}
