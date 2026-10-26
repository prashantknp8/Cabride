package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Entity.RideRequest;
import com.cabride.ride_service.Enums.BookingStatus;
import com.cabride.ride_service.Enums.RideRequestStatus;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.RideType;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
public class RideRepositoryIntegrationTest {
    @Container
    static final MySQLContainer<?> mysql=
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("ride_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url",mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }
    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RideBookingRepository rideBookingRepository;

    @Autowired
    private RideRequestRepository rideRequestRepository;

    @Test
    void saveRide_shouldPersistAndRetrieveRide(){
        UUID driverId= UUID.randomUUID();

        Ride ride= Ride.builder()
                .driverId(driverId)
                .rideType(RideType.SOLO)
                .status(RideStatus.ACCEPTED)
                .pickupLatitude(26.54)
                .pickupLongitude(80.35)
                .destinationLatitude(26.50)
                .destinationLongitude(80.40)
                .availableSeats(3)
                .build();

        Ride savedRide=rideRepository.saveAndFlush(ride);

        Ride retrieveRide=rideRepository.findById(savedRide.getId()).orElseThrow();

        assertEquals(savedRide.getId(),retrieveRide.getId());
        assertEquals(driverId,retrieveRide.getDriverId());
        assertEquals(RideStatus.ACCEPTED,retrieveRide.getStatus());
        assertEquals(3,retrieveRide.getAvailableSeats());
    }

    @Test
    void savedBooking_shouldRejectDuplicateRideRequestId() {
        UUID rideId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        RideBooking firstBooking = RideBooking.builder()
                .rideId(rideId)
                .passengerId(passengerId)
                .rideRequestId(requestId)
                .pickupLat(26.45)
                .pickupLon(80.35)
                .dropLat(26.50)
                .dropLon(80.40)
                .status(BookingStatus.CONFIRMED)
                .build();

        rideBookingRepository.saveAndFlush(firstBooking);

        RideBooking duplicateBooking = RideBooking.builder()
                .rideId(rideId)
                .passengerId(passengerId)
                .rideRequestId(requestId)
                .pickupLat(26.45)
                .pickupLon(80.35)
                .dropLat(26.50)
                .dropLat(80.40)
                .status(BookingStatus.CONFIRMED)
                .build();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> rideBookingRepository.saveAndFlush(duplicateBooking)
        );
    }

    @Test
    void findRequest_shouldRequiredMatchingPassengerAndStatus(){
        UUID passengerId=UUID.randomUUID();
        UUID anotherPassengerId=UUID.randomUUID();

        RideRequest request=RideRequest.builder()
                .passengerId(passengerId)
                .pickupLat(26.45)
                .pickupLon(80.45)
                .destinationLat(26.50)
                .destinationLon(80.50)
                .status(RideRequestStatus.DRIVER_ACCEPTED)
                .build();

        RideRequest savedRequest=rideRequestRepository.saveAndFlush(request);

        //correct passenger and status should find the request
        assertTrue(rideRequestRepository.findByIdAndPassengerIdAndStatus(savedRequest.getId(),passengerId,RideRequestStatus.DRIVER_ACCEPTED)
                .isPresent());

        //another passenger must not find it
        assertTrue(rideRequestRepository.findByIdAndPassengerIdAndStatus(savedRequest.getId(),anotherPassengerId,RideRequestStatus.DRIVER_ACCEPTED)
                .isEmpty());

        //Incorrect status must not find it
        assertTrue(rideRequestRepository.findByIdAndPassengerIdAndStatus(savedRequest.getId(),passengerId,RideRequestStatus.MATCHED)
                .isEmpty());
    }

    @Test
    void savedRideRequest_shouldPersistPassengerAndInitialStatus(){
        UUID passengerId=UUID.randomUUID();
        RideRequest request=RideRequest.builder()
                .passengerId(passengerId)
                .pickupLat(20.45)
                .pickupLon(80.45)
                .destinationLat(20.55)
                .destinationLon(80.55)
                .status(RideRequestStatus.REQUESTED)
                .build();

        RideRequest saved=rideRequestRepository.saveAndFlush(request);

        RideRequest retrieved=rideRequestRepository
                .findById(saved.getId())
                .orElseThrow();

        assertEquals(passengerId,retrieved.getPassengerId());
        assertEquals(20.45,retrieved.getPickupLat());
        assertEquals(80.45,retrieved.getPickupLon());
        assertEquals(20.55,retrieved.getDestinationLat());
        assertEquals(80.55,retrieved.getDestinationLon());
        assertEquals(RideRequestStatus.REQUESTED,retrieved.getStatus());

    }
    @Test
    void saveRide_shouldRejectMissingDriverId() {
        Ride ride = Ride.builder()
                .rideType(RideType.SOLO)
                .status(RideStatus.ACCEPTED)
                .pickupLatitude(26.45)
                .pickupLongitude(80.35)
                .destinationLatitude(26.50)
                .destinationLongitude(80.40)
                .availableSeats(3)
                .build();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> rideRepository.saveAndFlush(ride)
        );
    }

    @Test
    void saveRideRequest_shouldRejectMissingPassengerId() {
        RideRequest request = RideRequest.builder()
                .pickupLat(26.45)
                .pickupLon(80.35)
                .destinationLat(26.50)
                .destinationLon(80.40)
                .status(RideRequestStatus.REQUESTED)
                .build();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> rideRequestRepository.saveAndFlush(request)
        );
    }


}
