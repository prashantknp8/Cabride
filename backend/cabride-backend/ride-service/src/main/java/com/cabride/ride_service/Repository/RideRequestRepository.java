package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.RideRequest;
import com.cabride.ride_service.Enums.RideRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideRequestRepository
        extends JpaRepository<RideRequest, UUID> {

    List<RideRequest> findByPassengerId(UUID passengerId);

    List<RideRequest> findByStatus(RideRequestStatus status);
    Optional<RideRequest> findByIdAndPassengerId(
            UUID id,
            UUID passengerId
    );
    Optional<RideRequest> findByIdAndStatus(
            UUID id,
            RideRequestStatus status
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT r FROM RideRequest r
    WHERE r.id = :id
      AND r.passengerId = :passengerId
      AND r.status = :status
    """)
    Optional<RideRequest> findByIdAndPassengerIdAndStatus(
            @Param("id") UUID id,
            @Param("passengerId") UUID passengerId,
            @Param("status") RideRequestStatus status
    );
    List<RideRequest> findByPassengerIdAndStatus(
            UUID passengerId,
            RideRequestStatus status
    );
}