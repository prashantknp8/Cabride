package com.cabride.ride_service.Entity;

import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "route_change_proposal_stops",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_proposal_stop_sequence",
                        columnNames = {"proposal_id", "sequence_number"}
                )
        },
        indexes = {
                @Index(name = "idx_proposal_stop_proposal", columnList = "proposal_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteChangeProposalStop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "proposal_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID proposalId;

    // Null for newly inserted stops; populated for existing route stops.
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "source_stop_id", columnDefinition = "CHAR(36)")
    private UUID sourceStopId;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "booking_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false)
    private StopType stopType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StopStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}