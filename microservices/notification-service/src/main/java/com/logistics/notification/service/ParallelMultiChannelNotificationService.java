package com.logistics.notification.service;

import com.logistics.notification.config.AsyncNotificationThreadPoolConfig;
import com.logistics.notification.model.NotificationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Multi-Threaded Notification Service.
 * Dispatches notifications across multiple channels (EMAIL, SMS, ZALO_ZNS, PUSH)
 * simultaneously in parallel worker threads using CompletableFuture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelMultiChannelNotificationService {

    private final NotificationDispatcherService dispatcherService;

    @Qualifier(AsyncNotificationThreadPoolConfig.NOTIF_DISPATCH_EXECUTOR)
    private final Executor notificationTaskExecutor;

    /**
     * Broadcasts an urgent parcel update across multiple channels concurrently in parallel threads.
     *
     * @param channels List of target notification channels
     * @param recipient Recipient identifier (phone, email, device token)
     * @param title Title of notification
     * @param content Body message
     * @param trackingNumber Tracking number
     * @return List of persisted NotificationLog records
     */
    public List<NotificationLog> broadcastConcurrently(
            List<NotificationLog.Channel> channels,
            String recipient,
            String title,
            String content,
            String trackingNumber) {

        log.info("Spawning {} parallel worker threads to broadcast notification for tracking [{}]",
                channels.size(), trackingNumber);

        List<CompletableFuture<NotificationLog>> futures = channels.stream()
                .map(channel -> CompletableFuture.supplyAsync(() -> {
                    log.debug("[Thread: {}] Dispatching notification on channel [{}]",
                            Thread.currentThread().getName(), channel);
                    
                    NotificationLog logEntry = NotificationLog.builder()
                            .channel(channel)
                            .recipient(recipient)
                            .title(title)
                            .messageContent(content)
                            .trackingNumber(trackingNumber)
                            .build();

                    return dispatcherService.dispatch(logEntry);
                }, notificationTaskExecutor))
                .toList();

        // Barrier synchronization: wait for all parallel channel dispatches to finish
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<NotificationLog> results = new ArrayList<>();
        for (CompletableFuture<NotificationLog> f : futures) {
            try {
                results.add(f.get());
            } catch (Exception e) {
                log.error("Channel dispatch failed: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        log.info("Multi-threaded notification broadcast finished for tracking [{}] ({} dispatched)",
                trackingNumber, results.size());
        return results;
    }
}
