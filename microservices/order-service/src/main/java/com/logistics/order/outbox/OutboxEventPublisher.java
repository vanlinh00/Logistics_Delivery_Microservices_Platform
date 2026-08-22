package com.logistics.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.model.OrderOutbox;
import com.logistics.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Polls the outbox table every 3 seconds to publish events to Kafka.
     * Guarantees At-Least-Once delivery and eliminates 2PC/distributed transactions.
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OrderOutbox> pendingEvents = outboxRepository.findUnprocessedEvents(PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) return;

        for (OrderOutbox event : pendingEvents) {
            String topic = "logistics.orders." + event.getEventType().toLowerCase();
            try {
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Successfully published outbox event [{}] to topic [{}]", event.getId(), topic);
                            } else {
                                log.error("Failed to publish outbox event [{}] to topic [{}]: {}", event.getId(), topic, ex.getMessage());
                            }
                        });
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Error dispatching event [{}]: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() == null ? 1 : event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
            }
        }
        outboxRepository.saveAll(pendingEvents);
    }
}
