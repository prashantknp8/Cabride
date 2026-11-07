package com.cabride.ride_service.Service;

import com.cabride.ride_service.Dto.Request.CreateRouteChangeRequest;
import com.cabride.ride_service.Dto.Response.RouteChangeProposalResponse;

import java.util.UUID;

public interface RouteChangeProposalService {

    RouteChangeProposalResponse createProposal(
            CreateRouteChangeRequest request,
            UUID requestingPassengerId
    );

    RouteChangeProposalResponse submitConsent(
            UUID proposalId,
            UUID passengerId,
            boolean approved
    );

    RouteChangeProposalResponse getProposal(UUID proposalId);
}