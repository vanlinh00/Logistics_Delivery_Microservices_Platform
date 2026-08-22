package com.logistics.fleet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "drivers", indexes = {
    @Index(name = "idx_driver_code", columnList = "driverCode", unique = true),
    @Index(name = "idx_driver_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String driverCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String vehiclePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VehicleType vehicleType; // MOTORBIKE, VAN, TRUCK_1T, TRUCK_5T

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DriverStatus status; // AVAILABLE, ON_DUTY, BUSY, OFFLINE

    private Double currentLatitude;
    private Double currentLongitude;
    private String assignedHubId;

    private Integer activeAssignmentsCount;
    private Double rating;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum VehicleType {
        MOTORBIKE, VAN, TRUCK_1T, TRUCK_5T
    }

    public enum DriverStatus {
        AVAILABLE, ON_DUTY, BUSY, OFFLINE
    }
}
