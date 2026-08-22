package com.logistics.notification.repository;

import com.logistics.notification.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByRecipientOrderBySentAtDesc(String recipient);
    List<NotificationLog> findByTrackingNumber(String trackingNumber);
}
