package com.logistics.tracking.controller;

import com.logistics.tracking.model.TrackingEvent;
import com.logistics.tracking.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
@Tag(name = "Real-time Tracking & Telemetry", description = "Ingests GPS coordinates, checks geospatial proximity, and returns shipment journey history")
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/events")
    @Operation(summary = "Record new tracking waypoint or courier GPS ping")
    public ResponseEntity<TrackingEvent> recordEvent(@RequestBody TrackingEvent event) {
        return ResponseEntity.ok(trackingService.recordTrackingEvent(event));
    }

    @GetMapping("/{trackingNumber}")
    @Operation(summary = "Get full chronological timeline for a tracking number")
    public ResponseEntity<List<TrackingEvent>> getHistory(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(trackingService.getTrackingHistory(trackingNumber));
    }
}
