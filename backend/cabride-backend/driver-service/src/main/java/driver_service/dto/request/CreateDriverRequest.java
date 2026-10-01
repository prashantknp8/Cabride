package driver_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDriverRequest {
    @NotBlank(message = "License number is required")
    private String licenseNumber;
}
