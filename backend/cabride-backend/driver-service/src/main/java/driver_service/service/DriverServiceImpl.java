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
import org.springframework.transaction.annotation.Transactional;

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
        driver.setVehicleCapacity(request.getVehicleCapacity());

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
    public DriverResponse getDriverById(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver profile not found"
                ));

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

        if (requestedStatus == DriverStatus.RESERVED
                || requestedStatus == DriverStatus.ON_TRIP) {
            throw new InvalidStatusTransitionException(
                    "This status is managed by the ride workflow"
            );
        }

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

    @Override
    public DriverResponse startTrip(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Driver profile not found"
                ));
        if(driver.getStatus()!=DriverStatus.RESERVED){
            throw new InvalidStatusTransitionException(
                    "Only reserved drivers can start a trip"
            );
        }

        driver.setStatus(DriverStatus.ON_TRIP);

        return mapToResponse(driverRepository.save(driver));
    }

    @Override
    public DriverResponse reserveDriver(UUID driverId) {
        Driver driver=driverRepository.findById(driverId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Driver profile not found"
                ));

        if(driver.getStatus()!=DriverStatus.AVAILABLE){
            throw new InvalidStatusTransitionException(
                    "Only available drivers can be reserved"
            );
        }
        driver.setStatus(DriverStatus.RESERVED);

        return mapToResponse(driverRepository.save(driver));
    }

    @Override
    public DriverResponse releaseDriver(UUID driverId) {
        Driver driver=driverRepository.findById(driverId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Driver profile not found"
                ));

        if(driver.getStatus()!=DriverStatus.RESERVED){
            throw new InvalidStatusTransitionException(
                    "Only reserved drivers can be released"
            );
        }
        driver.setStatus(DriverStatus.AVAILABLE);

        return mapToResponse(driverRepository.save(driver));
    }

    @Override
    @Transactional
    public DriverResponse completeTrip(UUID driverId) {
       Driver driver= driverRepository.findById(driverId)
               .orElseThrow(()->new ResourceNotFoundException(
                       "Driver not found"
               ));

       if(driver.getStatus()!=DriverStatus.ON_TRIP){
           throw new InvalidStatusTransitionException(
                   "Only drivers currently on a trip can complete it"
           );
       }

       driver.setStatus(AVAILABLE);
       Driver savedDriver= driverRepository.save(driver);
       return mapToResponse(savedDriver);
    }

    private boolean isValidTransition(
            DriverStatus current,
            DriverStatus requested) {

        return switch (current) {

            case OFFLINE ->
                    requested == DriverStatus.AVAILABLE;

            case AVAILABLE ->
                    requested == DriverStatus.OFFLINE
                            || requested == DriverStatus.RESERVED;

            case RESERVED ->
                    requested == DriverStatus.AVAILABLE
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
                .vehicleCapacity(driver.getVehicleCapacity())
                .status(driver.getStatus())
                .rating(driver.getRating())
                .totalRides(driver.getTotalRides())
                .build();
    }
}
