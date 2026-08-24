package com.logistics.notification.strategy;

import com.logistics.notification.model.NotificationLog.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationStrategy implements NotificationChannelStrategy {

    @Override
    public Channel getSupportedChannel() {
        return Channel.EMAIL;
    }

    @Override
    public boolean sendNotification(String recipient, String title, String message, String trackingNumber) {
        log.info("📧 [EMAIL SMTP] Sending Email to [{}] for Tracking [{}]: Subject: {} | Body: {}",
                recipient, trackingNumber, title, message);
        // JavaMailSender SMTP dispatch simulation
        return true;
    }
}
