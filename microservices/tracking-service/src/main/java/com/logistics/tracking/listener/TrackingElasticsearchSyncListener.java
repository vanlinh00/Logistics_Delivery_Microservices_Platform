package com.logistics.tracking.listener;

import com.logistics.tracking.model.TrackingEvent;
import com.logistics.tracking.service.ParcelElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-Driven Change Data Capture (CDC) Consumer.
 * Ingests Kafka events from 'logistics.tracking.event-recorded' and asynchronously
 * updates the Elasticsearch index within milliseconds to keep search data 100% in sync with PostgreSQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingElasticsearchSyncListener {

    private final ParcelElasticsearchService elasticsearchService;

    @KafkaListener(topics = "logistics.tracking.event-recorded", groupId = "tracking-es-sync-group")
    public void handleTrackingEvent(String trackingNumber, String statusDescription) {
        log.info("CDC Kafka Event: Received tracking update for [{}], syncing to Elasticsearch...", trackingNumber);
        
        TrackingEvent event = TrackingEvent.builder()
                .trackingNumber(trackingNumber)
                .statusDescription(statusDescription)
                .build();
                
        elasticsearchService.syncTrackingEventToElasticsearch(event);
    }
}
