package driver_service.repository;

import driver_service.entity.Driver;
import driver_service.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByStatus(DriverStatus status);
}