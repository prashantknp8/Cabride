package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.RideBooking;
import com.cabride.ride_service.Enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RideBookingRepository extends JpaRepository<RideBooking, UUID> {

    List<RideBooking> findByPassengerId(UUID passengerId);

    List<RideBooking> findByRideId(UUID rideId);

    boolean existsByRideIdAndPassengerIdAndStatusIn(
            UUID rideId,
            UUID passengerId,
            List<BookingStatus> statuses
    );

    boolean existsByRideRequestId(UUID id);
}
