package com.logistics.fulfillment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "proof_of_deliveries", indexes = {
    @Index(name = "idx_pod_tracking_number", columnList = "trackingNumber", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofOfDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String recipientSignedName;

    private String recipientPhone;

    @Column(columnDefinition = "TEXT")
    private String signatureDataUri;

    @Column(columnDefinition = "TEXT")
    private String photoEvidenceUrl;

    private Double deliveryLatitude;
    private Double deliveryLongitude;

    private String courierId;
    private String courierNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryResult result; // SUCCESS, FAILED_ATTEMPT, REJECTED_BY_RECEIVER

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime deliveredAt;

    public enum DeliveryResult {
        SUCCESS,
        FAILED_ATTEMPT,
        REJECTED_BY_RECEIVER
    }
}
