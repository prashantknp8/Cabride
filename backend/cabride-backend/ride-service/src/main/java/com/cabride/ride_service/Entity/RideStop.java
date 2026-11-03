package com.cabride.ride_service.Entity;

import com.cabride.ride_service.Enums.StopStatus;
import com.cabride.ride_service.Enums.StopType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an individual pickup or drop-off location
 * within a ride's dynamically managed route.

 * Each RideBooking should have exactly two associated stops:
 * one PICKUP and one DROP.
 */
@Entity
@Table(
        name="ride_stops",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uk_ride_stop_sequence",
                        columnNames = {"ride_id","sequence_number"}
                )
        },
indexes ={
                        @Index(name = "idx_ride_stop_ride_id",columnList = "ride_id"),
                        @Index(name = "idx_ride_stop_booking_id",columnList = "booking_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideStop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    // Identifies the ride whose route contains this stop.
    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "ride_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID rideId;

    // Identifies the passenger booking associated with this stop.
    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "booking_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID bookingId;

    // Determines whether this location is a pickup or drop-off.
    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false)
    private StopType stopType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // Determines the order in which the driver should visit stops.
    @Column(name = "sequence_number", nullable = false)
    private Integer sequence;

    // Tracks the execution state of this individual stop.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StopStatus status;

    // Estimated arrival time, recalculated when the route changes.
    private LocalDateTime estimatedArrival;

    // Actual completion time, recorded when the stop is completed.
    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        // New stops must begin in the pending state.
        if (status == null) {
            status = StopStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}