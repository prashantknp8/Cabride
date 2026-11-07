package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.RouteChangeProposal;
import com.cabride.ride_service.Enums.RouteChangeProposalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteChangeProposalRepository
        extends JpaRepository<RouteChangeProposal, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM RouteChangeProposal p WHERE p.id = :proposalId")
    Optional<RouteChangeProposal> findByIdForUpdate(
            @Param("proposalId") UUID proposalId
    );

    List<RouteChangeProposal> findByRideIdAndStatusIn(
            UUID rideId,
            List<RouteChangeProposalStatus> statuses
    );

    Optional<RouteChangeProposal> findByIdAndRideId(
            UUID proposalId,
            UUID rideId
    );

    List<RouteChangeProposal> findByStatusAndExpiresAtBefore(
            RouteChangeProposalStatus status,
            LocalDateTime currentTime
    );

}