package com.logistics.fulfillment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hub_transit_records", indexes = {
    @Index(name = "idx_transit_tracking", columnList = "trackingNumber"),
    @Index(name = "idx_transit_hub", columnList = "sourceHubId, destinationHubId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubTransitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String trackingNumber;

    @Column(nullable = false)
    private String sourceHubId;

    @Column(nullable = false)
    private String destinationHubId;

    private String vehiclePlate;
    private String containerSealNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransitStatus status; // SORTED_AT_ORIGIN, LOADED_ON_TRUCK, IN_TRANSIT, ARRIVED_AT_DESTINATION_HUB, UNLOADED

    @CreationTimestamp
    private LocalDateTime scannedAt;

    public enum TransitStatus {
        SORTED_AT_ORIGIN,
        LOADED_ON_TRUCK,
        IN_TRANSIT,
        ARRIVED_AT_DESTINATION_HUB,
        UNLOADED
    }
}
