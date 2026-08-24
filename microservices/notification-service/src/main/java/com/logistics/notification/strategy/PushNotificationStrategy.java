package com.logistics.notification.strategy;

import com.logistics.notification.model.NotificationLog.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushNotificationStrategy implements NotificationChannelStrategy {

    @Override
    public Channel getSupportedChannel() {
        return Channel.PUSH;
    }

    @Override
    public boolean sendNotification(String recipient, String title, String message, String trackingNumber) {
        log.info("🔔 [FCM PUSH] Dispatching Firebase Cloud Message to Device Token [{}] for Tracking [{}]: {} - {}",
                recipient, trackingNumber, title, message);
        return true;
    }
}
