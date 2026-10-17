package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
}