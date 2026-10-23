package com.cabride.ride_service.Controller;


import com.cabride.ride_service.Dto.Request.CreateRideRequest;
import com.cabride.ride_service.Dto.Response.RideRequestResponse;
import com.cabride.ride_service.Service.RideRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ride-requests")
@RequiredArgsConstructor
public class RideRequestController {

    private final RideRequestService rideRequestService;

    //need user jwt
    @PostMapping
    public ResponseEntity<RideRequestResponse> createRequest(
            @Valid @RequestBody CreateRideRequest request,
            Authentication authentication
    ) {

        UUID passengerId = UUID.fromString(
                authentication.getName()
        );

        RideRequestResponse response =
                rideRequestService.createRequest(passengerId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<RideRequestResponse>> getMyRequests(
            Authentication authentication
    ) {
        UUID passengerId = UUID.fromString(
                authentication.getName()
        );

        return ResponseEntity.ok(
                rideRequestService.getMyRequests(passengerId)
        );
    }
//need driver jwt
    @PostMapping("/{requestId}/accept")
    public ResponseEntity<RideRequestResponse> acceptRequest(
            @PathVariable UUID requestId,
            @RequestHeader ("Authorization") String authorization
    ){
        String token = authorization.substring(7);

        RideRequestResponse response=
                rideRequestService.acceptRequest(requestId,token);
        return ResponseEntity.ok(response);
    }

    //need user jwt
    @PostMapping("/{requestId}/confirm")
    public ResponseEntity<RideRequestResponse> confirmRequest(
            @PathVariable UUID requestId,
            @RequestHeader("Authorization") String authorization
    ) {
        log.info(">>> CONFIRM ENDPOINT REACHED");
        UUID passengerId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        String token = authorization.substring(7);

        return ResponseEntity.ok(
                rideRequestService.confirmRequest(requestId, passengerId,token)
        );
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<RideRequestResponse> getRequestById(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        UUID passengerId = UUID.fromString(
                authentication.getName()
        );

        RideRequestResponse response =
                rideRequestService.getRequestById(requestId,passengerId);

        if (!response.getPassengerId().equals(passengerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(response);
    }
}
