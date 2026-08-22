package com.logistics.tracking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tracking_events", indexes = {
    @Index(name = "idx_track_event_number", columnList = "trackingNumber, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String trackingNumber;

    @Column(nullable = false)
    private String eventType; // PICKED_UP, HUB_ARRIVED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, GPS_PING

    @Column(nullable = false)
    private String statusDescription;

    private String locationName;
    private Double latitude;
    private Double longitude;

    private String actorName;
    private String actorRole; // DRIVER, HUB_MANAGER, SYSTEM

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
