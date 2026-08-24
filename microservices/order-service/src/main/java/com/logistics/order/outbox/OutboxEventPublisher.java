package com.logistics.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.config.AsyncThreadPoolConfig;
import com.logistics.order.model.OrderOutbox;
import com.logistics.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Qualifier(AsyncThreadPoolConfig.OUTBOX_EXECUTOR)
    private final Executor outboxTaskExecutor;

    /**
     * Polls the outbox table every 3 seconds to publish events to Kafka.
     * Utilizes Multi-Threading (CompletableFuture + ThreadPoolTaskExecutor) for parallel Kafka dispatching.
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OrderOutbox> pendingEvents = outboxRepository.findUnprocessedEvents(PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) return;

        log.debug("Dispatching {} pending outbox events across worker threads", pendingEvents.size());

        // Multi-threaded async processing for each pending outbox event
        List<CompletableFuture<Void>> futures = pendingEvents.stream()
                .map(event -> CompletableFuture.runAsync(() -> {
                    String topic = "logistics.orders." + event.getEventType().toLowerCase();
                    try {
                        log.debug("[Thread: {}] Publishing event [{}] to Kafka topic [{}]",
                                Thread.currentThread().getName(), event.getId(), topic);
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
                }, outboxTaskExecutor))
                .toList();

        // Wait for all parallel outbox dispatch tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        outboxRepository.saveAll(pendingEvents);
    }
}

