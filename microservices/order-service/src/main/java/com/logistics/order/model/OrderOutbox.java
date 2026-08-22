package com.logistics.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_outbox", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "processed, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String aggregateType; // "ORDER"

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType; // "ORDER_CREATED", "ORDER_STATUS_UPDATED", "ORDER_CANCELLED"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    private Integer retryCount;

    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
