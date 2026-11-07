package com.cabride.ride_service.Entity;

import com.cabride.ride_service.Enums.RouteChangeProposalStatus;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "route_change_proposals",
        indexes = {
                @Index(name = "idx_proposal_ride_id", columnList = "ride_id"),
                @Index(name = "idx_proposal_status", columnList = "status"),
                @Index(name = "idx_proposal_expiry", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteChangeProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "ride_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID rideId;

    // Booking whose new pickup/drop-off caused this route proposal.
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "requesting_booking_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID requestingBookingId;

    // Version of the active route from which this proposal was calculated.
    @Column(name = "base_route_version", nullable = false)
    private Long baseRouteVersion;

    // Version that will become active if all required passengers approve.
    @Column(name = "proposed_route_version", nullable = false)
    private Long proposedRouteVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteChangeProposalStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = RouteChangeProposalStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}