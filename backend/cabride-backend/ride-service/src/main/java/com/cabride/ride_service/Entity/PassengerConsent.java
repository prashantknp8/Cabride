package com.cabride.ride_service.Entity;

import com.cabride.ride_service.Enums.PassengerConsentStatus;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "passenger_consents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_proposal_passenger_booking",
                        columnNames = {"proposal_id", "booking_id"}
                )
        },
        indexes = {
                @Index(name = "idx_consent_passenger", columnList = "passenger_id"),
                @Index(name = "idx_consent_proposal", columnList = "proposal_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "proposal_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID proposalId;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "booking_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID bookingId;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "passenger_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID passengerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassengerConsentStatus status;

    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = PassengerConsentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}