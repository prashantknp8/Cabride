package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.RouteChangeProposalStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteChangeProposalStopRepository
        extends JpaRepository<RouteChangeProposalStop, UUID> {

    List<RouteChangeProposalStop> findByProposalIdOrderBySequenceAsc(
            UUID proposalId
    );

    void deleteByProposalId(UUID proposalId);
}