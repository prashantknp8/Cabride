package com.cabride.ride_service.Controller;

import com.cabride.ride_service.Dto.Request.CreateRouteChangeRequest;
import com.cabride.ride_service.Dto.Request.PassengerConsentRequest;
import com.cabride.ride_service.Dto.Response.RouteChangeProposalResponse;
import com.cabride.ride_service.Service.RouteChangeProposalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/route-change-proposals")
@RequiredArgsConstructor
public class RouteChangeProposalController {

    private final RouteChangeProposalService proposalService;

    /**
     * Creates a route-change proposal for a new passenger.
     */
    @PostMapping
    public ResponseEntity<RouteChangeProposalResponse> createProposal(
            @Valid @RequestBody CreateRouteChangeRequest request,
            Principal principal
    ) {
        // The authenticated principal must have a UUID as its subject/name.
        UUID passengerId = UUID.fromString(principal.getName());

        RouteChangeProposalResponse response =
                proposalService.createProposal(request, passengerId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Allows an authenticated passenger to approve or reject a proposal.
     */
    @PostMapping("/{proposalId}/consent")
    public ResponseEntity<RouteChangeProposalResponse> submitConsent(
            @PathVariable UUID proposalId,
            @Valid @RequestBody PassengerConsentRequest request,
            Principal principal
    ) {
        UUID passengerId = UUID.fromString(principal.getName());

        RouteChangeProposalResponse response =
                proposalService.submitConsent(
                        proposalId,
                        passengerId,
                        request.getApproved()
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the current state of a route-change proposal.
     */
    @GetMapping("/{proposalId}")
    public ResponseEntity<RouteChangeProposalResponse> getProposal(
            @PathVariable UUID proposalId
    ) {
        return ResponseEntity.ok(
                proposalService.getProposal(proposalId)
        );
    }
}