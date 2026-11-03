package com.cabride.ride_service.Dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatchRideRequest {
    @NotNull
    private UUID rideRequestId;
}
