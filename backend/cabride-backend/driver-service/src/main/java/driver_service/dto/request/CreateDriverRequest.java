package driver_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDriverRequest {
    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotNull(message = "Vehicle capacity is required")
    @Min(value = 1, message = "Vehicle capacity must be at least 1")
    private Integer vehicleCapacity;

}
