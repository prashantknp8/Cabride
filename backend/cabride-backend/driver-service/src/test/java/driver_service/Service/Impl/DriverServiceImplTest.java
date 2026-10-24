package driver_service.Service.Impl;

import driver_service.entity.Driver;
import driver_service.enums.DriverStatus;
import driver_service.exception.InvalidStatusTransitionException;
import driver_service.repository.DriverRepository;
import driver_service.service.DriverServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {
    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverServiceImpl driverService;

    @Test
    void reserveDriver_shouldChangeStatusToReserved(){
        //Arrange
        UUID driverId= UUID.randomUUID();

        Driver driver=new Driver();
        driver.setId(driverId);
        driver.setStatus(DriverStatus.AVAILABLE);

        when(driverRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //Act
        driverService.reserveDriver(driverId);

        //Assert
        assertEquals(DriverStatus.RESERVED,driver.getStatus());
        verify(driverRepository).save(driver);
    }
    @Test
    void reserveDriver_shouldRejectDriverNotAvailable() {
        // Arrange
        UUID driverId = UUID.randomUUID();

        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setStatus(DriverStatus.ON_TRIP);

        when(driverRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        // Act & Assert
        assertThrows(
                InvalidStatusTransitionException.class,
                () -> driverService.reserveDriver(driverId)
        );

        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void startTrip_shouldChangeStatusToOnTrip() {
        // Arrange
        UUID driverId = UUID.randomUUID();

        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setStatus(DriverStatus.RESERVED);

        when(driverRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        driverService.startTrip(driverId);

        // Assert
        assertEquals(DriverStatus.ON_TRIP, driver.getStatus());
        verify(driverRepository).save(driver);
    }

    @Test
    void completeTrip_shouldChangeStatusToAvailable() {
        // Arrange
        UUID driverId = UUID.randomUUID();

        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setStatus(DriverStatus.ON_TRIP);

        when(driverRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        driverService.completeTrip(driverId);

        // Assert
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
        verify(driverRepository).save(driver);
    }

    @Test
    void completeTrip_shouldRejectDriverNotOnTrip() {
        // Arrange
        UUID driverId = UUID.randomUUID();

        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setStatus(DriverStatus.AVAILABLE);

        when(driverRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        // Act & Assert
        assertThrows(
                InvalidStatusTransitionException.class,
                () -> driverService.completeTrip(driverId)
        );

        verify(driverRepository, never()).save(any(Driver.class));
    }
}
