package com.logistics.notification.listener;

import com.logistics.notification.constant.KafkaTopic;
import com.logistics.notification.model.NotificationLog;
import com.logistics.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationLogRepository repository;

    @KafkaListener(topics = KafkaTopic.ORDER_CREATED, groupId = "notification-group")
    public void handleOrderCreated(String message) {
        log.info("Received order created event from Kafka: {}", message);
        repository.save(NotificationLog.builder()
                .recipient("customer@example.com")
                .channel(NotificationLog.Channel.EMAIL)
                .title("Your order has been received successfully")
                .messageContent("Your logistics delivery shipment order has been accepted into the platform.")
                .status(NotificationLog.Status.SENT)
                .build());
    }

    @KafkaListener(topics = KafkaTopic.FULFILLMENT_DELIVERED, groupId = "notification-group")
    public void handleDelivered(String message) {
        log.info("Received package delivered event from Kafka: {}", message);
        repository.save(NotificationLog.builder()
                .recipient("0987654321")
                .channel(NotificationLog.Channel.SMS)
                .title("Delivery Successful")
                .messageContent("Package has been successfully handed over with recipient signature confirmation.")
                .status(NotificationLog.Status.SENT)
                .build());
    }
}
