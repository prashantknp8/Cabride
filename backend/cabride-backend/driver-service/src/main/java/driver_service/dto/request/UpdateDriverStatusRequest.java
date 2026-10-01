package driver_service.dto.request;

import driver_service.enums.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDriverStatusRequest {

    @NotNull(message = "Driver status is required")
    private DriverStatus status;
}