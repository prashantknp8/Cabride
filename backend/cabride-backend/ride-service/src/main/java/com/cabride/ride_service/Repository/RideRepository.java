package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.Ride;
import com.cabride.ride_service.Enums.RideStatus;
import com.cabride.ride_service.Enums.RideType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Ride r WHERE r.id = :rideId")
    Optional<Ride> findByIdForUpdate(@Param("rideId") UUID rideId);

    List<Ride> findByRideTypeAndStatusInAndAvailableSeatsGreaterThan(
        RideType rideType,
        List<RideStatus> statuses,
        Integer availableSeats
        );
}