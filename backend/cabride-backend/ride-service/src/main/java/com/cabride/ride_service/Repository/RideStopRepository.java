package com.cabride.ride_service.Repository;


import com.cabride.ride_service.Entity.RideStop;
import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideStopRepository extends JpaRepository<RideStop, UUID> {

    List<RideStop> findByRideIdOrderBySequenceAsc(UUID rideId);

    List<RideStop> findByBookingId(UUID bookingId);

    Optional<RideStop> findByBookingIdAndStopType(
            UUID bookingId,
            StopType stopType
    );

    List<RideStop> findByRideIdAndStatusOrderBySequenceAsc(
            UUID rideId,
            StopStatus status
    );

    boolean existsByRideIdAndSequence(UUID rideId, Integer sequence);
}