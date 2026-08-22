package com.logistics.fleet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pickup_tasks", indexes = {
    @Index(name = "idx_pickup_order_id", columnList = "orderId"),
    @Index(name = "idx_pickup_driver_id", columnList = "driverId"),
    @Index(name = "idx_pickup_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickupTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String trackingNumber;

    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PickupStatus status; // PENDING, ASSIGNED, EN_ROUTE_TO_PICKUP, ARRIVED_AT_SENDER, PICKED_UP, FAILED

    @Column(nullable = false)
    private String senderAddress;
    private Double senderLatitude;
    private Double senderLongitude;

    private LocalDateTime scheduledPickupTime;
    private LocalDateTime actualPickupTime;
    private String failureReason;
    private String proofImageUri;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum PickupStatus {
        PENDING,
        ASSIGNED,
        EN_ROUTE_TO_PICKUP,
        ARRIVED_AT_SENDER,
        PICKED_UP,
        FAILED
    }
}
