package com.logistics.tracking.service;

import com.logistics.tracking.config.AsyncTrackingThreadPoolConfig;
import com.logistics.tracking.model.TrackingEvent;
import com.logistics.tracking.repository.TrackingEventRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Multi-Threaded Tracking Aggregator.
 * Simultaneously pulls persistent database history and Redis fast geo cache in parallel threads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTrackingAggregatorService {

    private final TrackingEventRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Qualifier(AsyncTrackingThreadPoolConfig.TRACKING_EXECUTOR)
    private final Executor trackingTaskExecutor;

    @Data
    @Builder
    public static class TrackingSummary {
        private String trackingNumber;
        private List<TrackingEvent> history;
        private Object latestCachedEvent;
        private int totalCheckpoints;
        private boolean isLiveTracked;
    }

    /**
     * Executes parallel tasks to aggregate tracking timeline and live cache concurrently.
     */
    public TrackingSummary aggregateTrackingDataConcurrently(String trackingNumber) {
        log.info("Starting multi-threaded tracking aggregation for [{}]", trackingNumber);

        // Thread 1: Load complete history from Postgres DB
        CompletableFuture<List<TrackingEvent>> historyFuture = CompletableFuture.supplyAsync(() -> {
            log.debug("[Thread: {}] Loading history from DB for [{}]", Thread.currentThread().getName(), trackingNumber);
            return repository.findByTrackingNumberOrderByTimestampDesc(trackingNumber);
        }, trackingTaskExecutor);

        // Thread 2: Load latest live snapshot from Redis
        CompletableFuture<Object> cacheFuture = CompletableFuture.supplyAsync(() -> {
            log.debug("[Thread: {}] Loading live snapshot from Redis for [{}]", Thread.currentThread().getName(), trackingNumber);
            try {
                return redisTemplate.opsForValue().get("tracking:latest:" + trackingNumber);
            } catch (Exception e) {
                log.warn("Redis fetch error: {}", e.getMessage());
                return null;
            }
        }, trackingTaskExecutor);

        // Wait for both threads to finish
        CompletableFuture.allOf(historyFuture, cacheFuture).join();

        List<TrackingEvent> history = List.of();
        Object cachedSnapshot = null;

        try {
            history = historyFuture.get();
            cachedSnapshot = cacheFuture.get();
        } catch (Exception e) {
            log.error("Error in multi-threaded aggregation: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        return TrackingSummary.builder()
                .trackingNumber(trackingNumber)
                .history(history)
                .latestCachedEvent(cachedSnapshot)
                .totalCheckpoints(history.size())
                .isLiveTracked(cachedSnapshot != null || !history.isEmpty())
                .build();
    }
}
