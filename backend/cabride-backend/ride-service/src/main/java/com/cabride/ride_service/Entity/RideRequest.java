package com.cabride.ride_service.Entity;


import com.cabride.ride_service.Enums.RideRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "ride_requests",
        indexes = {
                @Index(name = "idx_request_passenger_id", columnList = "passenger_id"),
                @Index(name = "idx_request_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "passenger_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID passengerId;

    @Column(name = "pickup_lat", nullable = false)
    private Double pickupLat;

    @Column(name = "pickup_lon", nullable = false)
    private Double pickupLon;

    @Column(name = "destination_lat", nullable = false)
    private Double destinationLat;

    @Column(name = "destination_lon", nullable = false)
    private Double destinationLon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideRequestStatus status;

    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "accepted_driver_id", columnDefinition = "CHAR(36)")
    private UUID acceptedDriverId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = RideRequestStatus.REQUESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}