package driver_service.controller;

import driver_service.dto.request.CreateDriverRequest;
import driver_service.dto.request.UpdateDriverStatusRequest;
import driver_service.dto.response.DriverResponse;
import driver_service.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<DriverResponse> registerDriver(
            @Valid @RequestBody CreateDriverRequest request
            ){
        UUID userId=getCurrentUserId();
        DriverResponse response=driverService.registerDriver(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<DriverResponse> getMyDriverProfile(){
        UUID userId=getCurrentUserId();

        return ResponseEntity.ok(driverService.getMyDriverProfile(userId));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<DriverResponse> updateStatus(
            @Valid @RequestBody UpdateDriverStatusRequest request) {

        UUID userId = getCurrentUserId();

        return ResponseEntity.ok(
                driverService.updateStatus(userId, request)
        );
    }

    private UUID getCurrentUserId(){
        return (UUID) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

}
