package driver_service.service;

import driver_service.dto.request.CreateDriverRequest;
import driver_service.dto.request.UpdateDriverStatusRequest;
import driver_service.dto.response.DriverResponse;

import java.util.UUID;

public interface DriverService {
    DriverResponse registerDriver(UUID userId, CreateDriverRequest request);

    DriverResponse getMyDriverProfile(UUID userId);
    DriverResponse getDriverById(UUID driverId);
    DriverResponse updateStatus(
            UUID userId,
            UpdateDriverStatusRequest request
    );
    DriverResponse startTrip(UUID driverId);

    DriverResponse reserveDriver(UUID driverId);

    DriverResponse releaseDriver(UUID driverId);
    DriverResponse completeTrip(UUID driverId);
}
