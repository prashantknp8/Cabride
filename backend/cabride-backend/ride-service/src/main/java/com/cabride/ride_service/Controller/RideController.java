package com.cabride.ride_service.Controller;

import com.cabride.ride_service.Client.DriverClient;
import com.cabride.ride_service.Dto.Response.ApiResponse;
import com.cabride.ride_service.Dto.Response.DriverResponse;
import com.cabride.ride_service.Service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.Authenticator;
import java.util.UUID;

@RestController
@RequestMapping("api/rides")
@RequiredArgsConstructor
public class RideController {
    private final RideService rideService;
    private final DriverClient driverClient;

    // driver jwt
    @PostMapping("/{rideId}/start")
    public ResponseEntity<ApiResponse> startRide(
            @PathVariable UUID rideId,
            @RequestHeader("Authorization") String authorization,
            Authentication authentication
            ){
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Bearer token");
        }
        String token=authorization.substring(7);
        UUID userId=UUID.fromString(authentication.getName());
        DriverResponse driver=driverClient.getMyDriverProfile(token);
        rideService.startRide(rideId,driver.getId(),token);
        return ResponseEntity.ok(
                new ApiResponse("Ride started successfully")
        );
    }

    @PostMapping("/{rideId}/complete")
    public ResponseEntity<ApiResponse> completeRide(
            @PathVariable UUID rideId,
            @RequestHeader ("Authorization") String authorization){
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Bearer token");
        }
        String token=authorization.substring(7);
        DriverResponse driver=driverClient.getMyDriverProfile(token);
        rideService.completeRide(rideId,driver.getId(),token);
        return ResponseEntity.ok(
                new ApiResponse("Ride completed successfully")
        );

        }

    }


