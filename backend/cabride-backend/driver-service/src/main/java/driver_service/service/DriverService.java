package driver_service.service;

import driver_service.dto.request.CreateDriverRequest;
import driver_service.dto.request.UpdateDriverStatusRequest;
import driver_service.dto.response.DriverResponse;

import java.util.UUID;

public interface DriverService {
    DriverResponse registerDriver(UUID userId, CreateDriverRequest request);

    DriverResponse getMyDriverProfile(UUID userId);

    DriverResponse updateStatus(
            UUID userId,
            UpdateDriverStatusRequest request
    );
}
