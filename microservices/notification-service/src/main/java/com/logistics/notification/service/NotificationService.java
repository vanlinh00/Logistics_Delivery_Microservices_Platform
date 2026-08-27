package com.logistics.notification.service;

import com.logistics.notification.dto.NotificationDTOs;
import com.logistics.notification.model.NotificationLog;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationLog> getLogs(String recipient, String trackingNumber);

    NotificationLog getLogById(UUID id);

    NotificationLog sendManual(NotificationDTOs.SendNotificationRequest request);

    NotificationLog sendManual(NotificationLog log);

    List<NotificationLog> broadcastParallel(NotificationDTOs.BroadcastRequest request);
}
