package com.logistics.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "courier_profiles", indexes = {
    @Index(name = "idx_courier_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_courier_hub_id", columnList = "assignedHubId"),
    @Index(name = "idx_courier_kyc_status", columnList = "kycStatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 32)
    private String citizenId; // CCCD / National ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private VehicleType vehicleType = VehicleType.MOTORBIKE;

    @Column(length = 32)
    private String licensePlate;

    @Column(length = 64)
    private String assignedHubId; // e.g., HUB-TAN-BINH-01

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Builder.Default
    private Double rating = 5.0;

    @Builder.Default
    private Integer totalDeliveries = 0;

    @Builder.Default
    private Boolean isOnline = false;

    @Builder.Default
    private Double maxCapacityKg = 30.0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum VehicleType {
        MOTORBIKE,
        ELECTRIC_SCOOTER,
        VAN_500KG,
        TRUCK_1TON
    }

    public enum KycStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
