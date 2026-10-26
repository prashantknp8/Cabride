package driver_service.Repository;

import driver_service.entity.Driver;
import driver_service.enums.DriverStatus;
import driver_service.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
public class DriverRepositoryIntegrationTest {
    @Container
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("driver_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                mysql::getDriverClassName);
    }
        @Autowired
        private DriverRepository driverRepository;

    @Test
    void savedDriver_shouldPersistAndRetrieveDriver() {
        UUID userId = UUID.randomUUID();

        Driver driver = Driver.builder()
                .userId(userId)
                .licenseNumber("TEST-LICENSE-001")
                .status(DriverStatus.AVAILABLE)
                .vehicleCapacity(4)
                .rating(5.0)
                .totalRides(0)
                .build();

        Driver savedDriver=driverRepository.saveAndFlush(driver);

        Driver foundDriver=driverRepository.findById(savedDriver.getId())
                .orElseThrow();

        assertNotNull(foundDriver.getId());
        assertEquals(userId, foundDriver.getUserId());
        assertEquals("TEST-LICENSE-001",foundDriver.getLicenseNumber());
        assertEquals(DriverStatus.AVAILABLE,foundDriver.getStatus());
        assertEquals(4,foundDriver.getVehicleCapacity());

    }
    @Test

void updateDriverStatus_shouldPersistNewStatus(){
        Driver driver= Driver.builder()
                .userId(UUID.randomUUID())
                .licenseNumber("TEST-LICENSE-002")
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        Driver savedDriver=driverRepository.saveAndFlush(driver);

        savedDriver.setStatus(DriverStatus.RESERVED);
        driverRepository.saveAndFlush(savedDriver);

        Driver updatedDriver=driverRepository.findById(savedDriver.getId()).orElseThrow();

        assertEquals(DriverStatus.RESERVED,updatedDriver.getStatus());
    }

    @Test
    void saveDriver_shouldRejectDuplicateLicenseNumber() {
        String licenseNumber = "DUPLICATE-LICENSE-001";

        Driver firstDriver = Driver.builder()
                .userId(UUID.randomUUID())
                .licenseNumber(licenseNumber)
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        driverRepository.saveAndFlush(firstDriver);

        Driver secondDriver = Driver.builder()
                .userId(UUID.randomUUID())
                .licenseNumber(licenseNumber)
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> driverRepository.saveAndFlush(secondDriver)
        );
    }


    @Test
    void saveDriver_shouldRejectDuplicateUserId() {
        UUID userId = UUID.randomUUID();

        Driver firstDriver = Driver.builder()
                .userId(userId)
                .licenseNumber("USER-UNIQUE-001")
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        driverRepository.saveAndFlush(firstDriver);

        Driver secondDriver = Driver.builder()
                .userId(userId)
                .licenseNumber("USER-UNIQUE-002")
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> driverRepository.saveAndFlush(secondDriver)
        );
    }


    @Test
    void saveDriver_shouldRejectMissingUserId() {
        Driver driver = Driver.builder()
                .licenseNumber("MISSING-USER-001")
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        assertThrows(
                Exception.class,
                () -> driverRepository.saveAndFlush(driver)
        );
    }


    @Test
    void saveDriver_shouldRejectMissingLicenseNumber() {
        Driver driver = Driver.builder()
                .userId(UUID.randomUUID())
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        assertThrows(
                Exception.class,
                () -> driverRepository.saveAndFlush(driver)
        );
    }


    @Test
    void saveDriver_shouldRejectMissingVehicleCapacity() {
        Driver driver = Driver.builder()
                .userId(UUID.randomUUID())
                .licenseNumber("MISSING-CAPACITY-001")
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        assertThrows(
                Exception.class,
                () -> driverRepository.saveAndFlush(driver)
        );
    }


    @Test
    void reserveAndReleaseDriver_shouldPersistStatusTransitions() {
        Driver driver = Driver.builder()
                .userId(UUID.randomUUID())
                .licenseNumber("RESERVE-RELEASE-001")
                .vehicleCapacity(4)
                .status(DriverStatus.AVAILABLE)
                .rating(5.0)
                .totalRides(0)
                .build();

        Driver savedDriver = driverRepository.saveAndFlush(driver);

        savedDriver.setStatus(DriverStatus.RESERVED);
        driverRepository.saveAndFlush(savedDriver);

        Driver reservedDriver = driverRepository.findById(savedDriver.getId())
                .orElseThrow();

        assertEquals(DriverStatus.RESERVED, reservedDriver.getStatus());

        reservedDriver.setStatus(DriverStatus.AVAILABLE);
        driverRepository.saveAndFlush(reservedDriver);

        Driver releasedDriver = driverRepository.findById(savedDriver.getId())
                .orElseThrow();

        assertEquals(DriverStatus.AVAILABLE, releasedDriver.getStatus());
    }


    @Test
    void startAndCompleteTrip_shouldPersistStatusTransitions() {
        Driver driver = Driver.builder()
                .userId(UUID.randomUUID())
                .licenseNumber("TRIP-LIFECYCLE-001")
                .vehicleCapacity(4)
                .status(DriverStatus.RESERVED)
                .rating(5.0)
                .totalRides(0)
                .build();

        Driver savedDriver = driverRepository.saveAndFlush(driver);

        savedDriver.setStatus(DriverStatus.ON_TRIP);
        driverRepository.saveAndFlush(savedDriver);

        Driver driverOnTrip = driverRepository.findById(savedDriver.getId())
                .orElseThrow();

        assertEquals(DriverStatus.ON_TRIP, driverOnTrip.getStatus());

        driverOnTrip.setStatus(DriverStatus.AVAILABLE);
        driverRepository.saveAndFlush(driverOnTrip);

        Driver completedDriver = driverRepository.findById(savedDriver.getId())
                .orElseThrow();

        assertEquals(DriverStatus.AVAILABLE, completedDriver.getStatus());
    }
















}
