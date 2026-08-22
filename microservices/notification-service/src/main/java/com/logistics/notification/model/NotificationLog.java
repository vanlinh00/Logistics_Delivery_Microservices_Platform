package com.logistics.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notif_recipient", columnList = "recipient"),
    @Index(name = "idx_notif_tracking", columnList = "trackingNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipient; // Email or Phone

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel; // SMS, EMAIL, PUSH, ZALO_ZNS

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // SENT, FAILED, QUEUED

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sentAt;

    public enum Channel {
        SMS, EMAIL, PUSH, ZALO_ZNS
    }

    public enum Status {
        SENT, FAILED, QUEUED
    }
}
