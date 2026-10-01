package driver_service.entity;

import driver_service.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.sql.Types.CHAR;

@Entity
@Data
@Table(name= "drivers",
uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id"),
        @UniqueConstraint(columnNames="license_number")
        })
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(CHAR)
    private UUID id;

    @Column(name="user_id",nullable = false)
    @JdbcTypeCode(CHAR)
    private UUID userId;

    @Column(name = "license_number",nullable = false,unique = true)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status=DriverStatus.OFFLINE;

    @Column(nullable = false)
    private Double rating=5.0;

    @Column(nullable = false)
    private Integer totalRides=0;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }









}
