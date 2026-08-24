package com.logistics.order.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ⚡ Kafka Change Data Capture (CDC) Consumer for Elasticsearch Sync
 * 
 * ❓ WHEN DO WE SAVE TO ELASTICSEARCH?
 * 1. When an Order is Created (status = PENDING/CONFIRMED).
 * 2. When an Order changes status (SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED).
 * 3. When Recipient details/Delivery Address are updated.
 * 
 * 💡 Pattern: Transactional Outbox + Kafka -> Consumer writes to Elasticsearch.
 * This guarantees:
 * - PostgreSQL is ALWAYS the Single Source of Truth (SSOT).
 * - Elasticsearch is eventually consistent with near zero lag (< 10ms).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderElasticsearchSyncConsumer {

    private final OrderElasticsearchService elasticsearchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.events", groupId = "order-es-indexer-group")
    public void onOrderEventReceived(String messageJson) {
        try {
            log.info("📩 Received order event from Kafka: {}", messageJson);
            OrderDocument orderDoc = objectMapper.readValue(messageJson, OrderDocument.class);

            // Ingest / Update Elasticsearch Document
            elasticsearchService.syncOrderToElasticsearch(orderDoc);
            log.info("✅ Successfully synchronized Order `{}` to Elasticsearch index `logistics_orders`", orderDoc.getId());
        } catch (Exception e) {
            log.error("❌ Failed to process order sync event: {}", e.getMessage(), e);
        }
    }
}
