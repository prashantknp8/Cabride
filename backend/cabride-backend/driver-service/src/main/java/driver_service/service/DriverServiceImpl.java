package driver_service.service;

import driver_service.dto.request.CreateDriverRequest;
import driver_service.dto.request.UpdateDriverStatusRequest;
import driver_service.dto.response.DriverResponse;
import driver_service.entity.Driver;
import driver_service.enums.DriverStatus;
import driver_service.exception.InvalidStatusTransitionException;
import driver_service.exception.ResourceAlreadyExistsException;
import driver_service.exception.ResourceNotFoundException;
import driver_service.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static driver_service.enums.DriverStatus.AVAILABLE;
import static driver_service.enums.DriverStatus.OFFLINE;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {
    private final DriverRepository driverRepository;

    @Override
    public DriverResponse registerDriver(UUID userId, CreateDriverRequest request) {
        if (driverRepository.existsByUserId(userId)) {
            throw new ResourceAlreadyExistsException(
                    "Driver profile already exists for this user ."
            );
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ResourceAlreadyExistsException(
                    "License Number already exists ."
            );
        }

        Driver driver = new Driver();
        driver.setUserId(userId);
        driver.setLicenseNumber(request.getLicenseNumber());

        // default values are already defined

        Driver savedDriver = driverRepository.save(driver);
        return mapToResponse(savedDriver);
    }

    @Override
    public DriverResponse getMyDriverProfile(UUID userId) {
        Driver driver = driverRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));


        return mapToResponse(driver);
    }

    @Override
    public DriverResponse updateStatus(
            UUID userId,
            UpdateDriverStatusRequest request) {

        Driver driver = driverRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver profile not found."
                        ));

        DriverStatus currentStatus = driver.getStatus();
        DriverStatus requestedStatus = request.getStatus();

        if (!isValidTransition(currentStatus, requestedStatus)) {
            throw new InvalidStatusTransitionException(
                    "Invalid driver status transition: "
                            + currentStatus
                            + " → "
                            + requestedStatus
            );
        }

        driver.setStatus(requestedStatus);

        Driver updatedDriver = driverRepository.save(driver);

        return mapToResponse(updatedDriver);
    }

    private boolean isValidTransition(
            DriverStatus current,
            DriverStatus requested) {

        return switch (current) {

            case OFFLINE ->
                    requested == DriverStatus.AVAILABLE;

            case AVAILABLE ->
                    requested == DriverStatus.OFFLINE
                            || requested == DriverStatus.ON_TRIP;

            case ON_TRIP ->
                    requested == DriverStatus.AVAILABLE;

            case SUSPENDED ->
                    false;
        };
    }

    private DriverResponse mapToResponse(Driver driver) {

        return DriverResponse.builder()
                .id(driver.getId())
                .userId(driver.getUserId())
                .licenseNumber(driver.getLicenseNumber())
                .status(driver.getStatus())
                .rating(driver.getRating())
                .totalRides(driver.getTotalRides())
                .build();
    }
}
