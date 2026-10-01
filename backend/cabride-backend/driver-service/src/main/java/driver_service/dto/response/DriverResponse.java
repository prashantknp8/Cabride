package driver_service.dto.response;

import driver_service.enums.DriverStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DriverResponse {
    private UUID id;
    private UUID userId;
    private String licenseNumber;
    private DriverStatus status;
    private Double rating;
    private Integer totalRides;
}
