package com.cabride.ride_service.Repository;

import com.cabride.ride_service.Entity.PassengerConsent;
import com.cabride.ride_service.Enums.PassengerConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassengerConsentRepository
        extends JpaRepository<PassengerConsent, UUID> {

    List<PassengerConsent> findByProposalId(UUID proposalId);

    List<PassengerConsent> findByProposalIdAndStatus(
            UUID proposalId,
            PassengerConsentStatus status
    );

    Optional<PassengerConsent> findByProposalIdAndPassengerId(
            UUID proposalId,
            UUID passengerId
    );

    boolean existsByProposalIdAndPassengerId(UUID proposalId, UUID passengerId);

    long countByProposalIdAndStatus(
            UUID proposalId,
            PassengerConsentStatus status
    );
}