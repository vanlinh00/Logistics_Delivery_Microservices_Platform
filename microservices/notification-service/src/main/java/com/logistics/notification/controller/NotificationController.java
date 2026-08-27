package com.logistics.notification.controller;

import com.logistics.notification.constant.ApiPath;
import com.logistics.notification.dto.ApiResponse;
import com.logistics.notification.dto.NotificationDTOs;
import com.logistics.notification.model.NotificationLog;
import com.logistics.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.NOTIFICATIONS_BASE)
@RequiredArgsConstructor
@Tag(name = "Notification & Alert Service", description = "Endpoints for triggering and querying customer alert logs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(ApiPath.LOGS)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'OPERATOR', 'COURIER', 'CUSTOMER') or isAuthenticated()")
    @Operation(summary = "List recent notification logs by recipient or tracking number")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getLogs(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String trackingNumber) {
        List<NotificationLog> logs = notificationService.getLogs(recipient, trackingNumber);
        return ResponseEntity.ok(ApiResponse.ok(logs, "Notification logs retrieved successfully"));
    }

    @GetMapping(ApiPath.LOGS_BY_ID)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'OPERATOR', 'COURIER', 'CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Get single notification log by UUID")
    public ResponseEntity<ApiResponse<NotificationLog>> getLogById(@PathVariable UUID id) {
        NotificationLog log = notificationService.getLogById(id);
        return ResponseEntity.ok(ApiResponse.ok(log, "Notification log retrieved successfully"));
    }

    @PostMapping(ApiPath.SEND_MANUAL)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'OPERATOR') or isAuthenticated()")
    @Operation(summary = "Dispatch urgent SMS / Email / Zalo alert manually using Strategy Pattern")
    public ResponseEntity<ApiResponse<NotificationLog>> sendManual(
            @Valid @RequestBody NotificationDTOs.SendNotificationRequest request) {
        NotificationLog result = notificationService.sendManual(request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Notification dispatched successfully"));
    }

    @PostMapping(ApiPath.BROADCAST_PARALLEL)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'OPERATOR') or isAuthenticated()")
    @Operation(summary = "Broadcast alert to all channels (EMAIL, SMS, ZALO_ZNS, PUSH) simultaneously via multi-threading")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> broadcastParallel(
            @Valid @RequestBody NotificationDTOs.BroadcastRequest request) {
        List<NotificationLog> results = notificationService.broadcastParallel(request);
        return ResponseEntity.ok(ApiResponse.ok(results, "Multi-threaded broadcast executed across " + results.size() + " worker threads"));
    }
}
