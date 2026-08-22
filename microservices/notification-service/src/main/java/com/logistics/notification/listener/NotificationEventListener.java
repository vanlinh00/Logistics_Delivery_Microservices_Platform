package com.logistics.notification.listener;

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

    @KafkaListener(topics = "logistics.orders.order_created", groupId = "notification-group")
    public void handleOrderCreated(String message) {
        log.info("Received order created event from Kafka: {}", message);
        repository.save(NotificationLog.builder()
                .recipient("customer@example.com")
                .channel(NotificationLog.Channel.EMAIL)
                .title("Đơn hàng của bạn đã được tiếp nhận thành công")
                .messageContent("Đơn hàng vận chuyển của bạn đã được tạo trên hệ thống Logistics.")
                .status(NotificationLog.Status.SENT)
                .build());
    }

    @KafkaListener(topics = "logistics.fulfillment.delivered", groupId = "notification-group")
    public void handleDelivered(String message) {
        log.info("Received package delivered event: {}", message);
        repository.save(NotificationLog.builder()
                .recipient("0987654321")
                .channel(NotificationLog.Channel.SMS)
                .title("Giao hàng thành công")
                .messageContent("Kiện hàng đã được phát thành công kèm chữ ký người nhận.")
                .status(NotificationLog.Status.SENT)
                .build());
    }
}
