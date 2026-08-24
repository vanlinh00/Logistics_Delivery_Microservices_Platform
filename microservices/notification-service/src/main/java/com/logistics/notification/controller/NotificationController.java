package com.logistics.notification.controller;

import com.logistics.notification.dto.ApiResponse;
import com.logistics.notification.model.NotificationLog;
import com.logistics.notification.repository.NotificationLogRepository;
import com.logistics.notification.service.NotificationDispatcherService;
import com.logistics.notification.service.ParallelMultiChannelNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification & Alert Service", description = "Endpoints for triggering and querying customer alert logs")
public class NotificationController {

    private final NotificationLogRepository repository;
    private final NotificationDispatcherService dispatcherService;
    private final ParallelMultiChannelNotificationService parallelNotificationService;

    @GetMapping("/logs")
    @Operation(summary = "List recent notification logs by recipient")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getLogs(@RequestParam(required = false) String recipient) {
        if (recipient != null) {
            return ResponseEntity.ok(ApiResponse.ok(repository.findByRecipientOrderBySentAtDesc(recipient), "Logs retrieved"));
        }
        return ResponseEntity.ok(ApiResponse.ok(repository.findAll(), "All logs retrieved"));
    }

    @PostMapping("/send-manual")
    @Operation(summary = "Dispatch urgent SMS / Email / Zalo alert manually using Strategy Pattern")
    public ResponseEntity<ApiResponse<NotificationLog>> sendManual(@RequestBody NotificationLog log) {
        if (log.getChannel() == null) {
            log.setChannel(NotificationLog.Channel.SMS);
        }
        NotificationLog result = dispatcherService.dispatch(log);
        return ResponseEntity.ok(ApiResponse.ok(result, "Notification dispatched successfully"));
    }

    @PostMapping("/broadcast-parallel")
    @Operation(summary = "Broadcast alert to all channels (EMAIL, SMS, ZALO_ZNS, PUSH) simultaneously via multi-threading")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> broadcastParallel(@RequestBody BroadcastRequest request) {
        List<NotificationLog.Channel> channels = request.getChannels() != null && !request.getChannels().isEmpty()
                ? request.getChannels()
                : List.of(NotificationLog.Channel.EMAIL, NotificationLog.Channel.SMS, NotificationLog.Channel.ZALO_ZNS, NotificationLog.Channel.PUSH);

        List<NotificationLog> results = parallelNotificationService.broadcastConcurrently(
                channels,
                request.getRecipient(),
                request.getTitle(),
                request.getContent(),
                request.getTrackingNumber()
        );
        return ResponseEntity.ok(ApiResponse.ok(results, "Multi-threaded broadcast executed across " + results.size() + " worker threads"));
    }

    @Data
    public static class BroadcastRequest {
        private List<NotificationLog.Channel> channels;
        private String recipient;
        private String title;
        private String content;
        private String trackingNumber;
    }
}


