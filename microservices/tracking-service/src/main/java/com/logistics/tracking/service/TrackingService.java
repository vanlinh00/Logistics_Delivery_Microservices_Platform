package com.logistics.tracking.service;

import com.logistics.tracking.model.TrackingEvent;
import com.logistics.tracking.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingEventRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String GEO_KEY_DRIVERS = "geo:couriers:live";

    @Transactional
    public TrackingEvent recordTrackingEvent(TrackingEvent event) {
        TrackingEvent saved = repository.save(event);

        // Update real-time cache in Redis (latest event for fast lookup)
        redisTemplate.opsForValue().set("tracking:latest:" + event.getTrackingNumber(), saved);

        // If GPS coordinates present, update Redis Geo spatial index
        if (event.getLatitude() != null && event.getLongitude() != null) {
            redisTemplate.opsForGeo().add(GEO_KEY_DRIVERS, new Point(event.getLongitude(), event.getLatitude()), event.getTrackingNumber());
        }

        // Publish to tracking stream
        kafkaTemplate.send("logistics.tracking.event-recorded", event.getTrackingNumber(), event.getStatusDescription());

        return saved;
    }

    public List<TrackingEvent> getTrackingHistory(String trackingNumber) {
        return repository.findByTrackingNumberOrderByTimestampDesc(trackingNumber);
    }
}
