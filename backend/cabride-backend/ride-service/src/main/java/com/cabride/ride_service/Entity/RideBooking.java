package com.cabride.ride_service.Entity;

import com.cabride.ride_service.Enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="ride_bookings",
indexes={
        @Index(name="idx_booking_ride_id", columnList="ride_id"),
        @Index(name="idx_booking_passenger_id", columnList="passenger_id")
      }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "ride_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID rideId;

    @JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "passenger_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID passengerId;

    @JdbcTypeCode(Types.CHAR)
    @Column(
            name = "ride_request_id",
            nullable = false,
            unique = true,
            columnDefinition = "CHAR(36)"
    )
    private UUID rideRequestId;


    @Column(nullable = false)
    private Double pickupLat;

    @Column(nullable = false)
    private Double pickupLon;

    @Column(nullable = false)
    private Double dropLat;

    @Column(nullable = false)
    private Double dropLon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(precision = 10, scale = 2)
    private BigDecimal fare;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = BookingStatus.REQUESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}




























