package com.logistics.notification.controller;

import com.logistics.notification.model.NotificationLog;
import com.logistics.notification.repository.NotificationLogRepository;
import com.logistics.notification.service.NotificationDispatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @GetMapping("/logs")
    @Operation(summary = "List recent notification logs by recipient")
    public ResponseEntity<List<NotificationLog>> getLogs(@RequestParam(required = false) String recipient) {
        if (recipient != null) {
            return ResponseEntity.ok(repository.findByRecipientOrderBySentAtDesc(recipient));
        }
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/send-manual")
    @Operation(summary = "Dispatch urgent SMS / Email / Zalo alert manually using Strategy Pattern")
    public ResponseEntity<NotificationLog> sendManual(@RequestBody NotificationLog log) {
        if (log.getChannel() == null) {
            log.setChannel(NotificationLog.Channel.SMS);
        }
        NotificationLog result = dispatcherService.dispatch(log);
        return ResponseEntity.ok(result);
    }
}

