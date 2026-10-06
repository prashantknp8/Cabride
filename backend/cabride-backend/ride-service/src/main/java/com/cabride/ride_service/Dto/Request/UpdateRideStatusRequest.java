package com.cabride.ride_service.Dto.Request;

import com.cabride.ride_service.Enums.RideStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRideRequest {
    @NotNull(message = "Ride status is required")
    private RideStatus status;
}
