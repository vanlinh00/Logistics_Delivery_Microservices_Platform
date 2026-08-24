package com.logistics.notification.strategy;

import com.logistics.notification.model.NotificationLog.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsNotificationStrategy implements NotificationChannelStrategy {

    @Override
    public Channel getSupportedChannel() {
        return Channel.SMS;
    }

    @Override
    public boolean sendNotification(String recipient, String title, String message, String trackingNumber) {
        log.info("📱 [SMS GATEWAY] Sending SMS to [{}] for Tracking [{}]: {} - {}",
                recipient, trackingNumber, title, message);
        // SMS Gateway dispatch simulation (Twilio / eSMS)
        return true;
    }
}
