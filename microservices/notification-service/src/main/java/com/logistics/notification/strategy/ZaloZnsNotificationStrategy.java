package com.logistics.notification.strategy;

import com.logistics.notification.model.NotificationLog.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ZaloZnsNotificationStrategy implements NotificationChannelStrategy {

    @Override
    public Channel getSupportedChannel() {
        return Channel.ZALO_ZNS;
    }

    @Override
    public boolean sendNotification(String recipient, String title, String message, String trackingNumber) {
        log.info("💬 [ZALO OFFICIAL OA] Dispatching ZNS Template to [{}] for Tracking [{}]: {} - {}",
                recipient, trackingNumber, title, message);
        // Zalo OA ZNS HTTPS API dispatch simulation
        return true;
    }
}
