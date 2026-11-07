package com.cabride.ride_service.Service;

import com.cabride.ride_service.Entity.RouteChangeProposal;
import com.cabride.ride_service.Enums.RouteChangeProposalStatus;
import com.cabride.ride_service.Repository.RouteChangeProposalRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RouteProposalExpiryScheduler {

    private final RouteChangeProposalRepository proposalRepository;

    /**
     * Expires proposals that have passed their consent deadline.
     * The active ride route remains untouched.
     */
    @Scheduled(fixedDelayString = "${cabride.route-consent.expiry-check-ms:60000}")
    @Transactional
    public void expirePendingProposals() {

        List<RouteChangeProposal> expiredProposals =
                proposalRepository.findByStatusAndExpiresAtBefore(
                        RouteChangeProposalStatus.PENDING,
                        LocalDateTime.now()
                );

        for (RouteChangeProposal proposal : expiredProposals) {
            proposal.setStatus(RouteChangeProposalStatus.EXPIRED);
        }

        proposalRepository.saveAll(expiredProposals);
    }
}