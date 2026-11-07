package com.cabride.ride_service.Dto.Response;

import com.cabride.ride_service.Enums.RouteChangeProposalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RouteChangeProposalResponse(
        UUID proposalId,
        UUID rideId,
        Long baseRouteVersion,
        Long proposedRouteVersion,
        RouteChangeProposalStatus status,
        LocalDateTime expiresAt,
        String message
) {}