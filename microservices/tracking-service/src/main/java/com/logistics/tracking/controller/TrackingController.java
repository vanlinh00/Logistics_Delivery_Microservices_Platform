package com.logistics.tracking.controller;

import com.logistics.tracking.dto.ApiResponse;
import com.logistics.tracking.model.TrackingEvent;
import com.logistics.tracking.service.AsyncTrackingAggregatorService;
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
    private final AsyncTrackingAggregatorService asyncAggregatorService;

    @PostMapping("/events")
    @Operation(summary = "Record new tracking waypoint or courier GPS ping")
    public ResponseEntity<ApiResponse<TrackingEvent>> recordEvent(@RequestBody TrackingEvent event) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.recordTrackingEvent(event), "Event recorded"));
    }

    @GetMapping("/{trackingNumber}")
    @Operation(summary = "Get full chronological timeline for a tracking number")
    public ResponseEntity<ApiResponse<List<TrackingEvent>>> getHistory(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTrackingHistory(trackingNumber), "Tracking history retrieved"));
    }

    @GetMapping("/aggregate-concurrently/{trackingNumber}")
    @Operation(summary = "Multi-threaded aggregation: simultaneously queries Postgres history and Redis real-time telemetry")
    public ResponseEntity<ApiResponse<AsyncTrackingAggregatorService.TrackingSummary>> aggregateConcurrently(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.ok(asyncAggregatorService.aggregateTrackingDataConcurrently(trackingNumber), "Aggregated concurrently across threads"));
    }
}

