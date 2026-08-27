package com.logistics.fulfillment.controller;

import com.logistics.fulfillment.constant.ApiPath;
import com.logistics.fulfillment.constant.MessageCode;
import com.logistics.fulfillment.dto.ApiResponse;
import com.logistics.fulfillment.dto.FulfillmentDTOs;
import com.logistics.fulfillment.model.HubTransitRecord;
import com.logistics.fulfillment.model.ProofOfDelivery;
import com.logistics.fulfillment.service.FulfillmentService;
import com.logistics.fulfillment.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 📦 FulfillmentController:
 * REST API for Proof of Delivery (POD) registration and logistics hub transit scanning.
 * All core domain logic is delegated to FulfillmentService.
 */
@RestController
@RequestMapping(ApiPath.FULFILLMENT_BASE)
@RequiredArgsConstructor
@Tag(name = "Delivery Fulfillment & Hub Transit", description = "Endpoints for hub package sorting, linehaul transit, and POD submission")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;
    private final MessageService messageService;

    @PostMapping(ApiPath.POD)
    @PreAuthorize("hasAnyRole('COURIER', 'DRIVER', 'ADMIN', 'DISPATCHER') or isAuthenticated()")
    @Operation(summary = "Submit Proof of Delivery (POD) with signature and photo attachment")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> submitPOD(@Valid @RequestBody FulfillmentDTOs.PodSubmitRequest request) {
        ProofOfDelivery saved = fulfillmentService.submitProofOfDelivery(request);
        return ResponseEntity.ok(ApiResponse.ok(saved, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @GetMapping(ApiPath.POD_BY_TRACKING)
    @Operation(summary = "Get Proof of Delivery details by tracking number")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> getPOD(@PathVariable String trackingNumber) {
        ProofOfDelivery pod = fulfillmentService.getProofOfDelivery(trackingNumber);
        return ResponseEntity.ok(ApiResponse.ok(pod, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @PostMapping(ApiPath.HUB_TRANSIT_SCAN)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN', 'DISPATCHER', 'HUB_MANAGER') or isAuthenticated()")
    @Operation(summary = "Scan package checkpoint at logistics hub")
    public ResponseEntity<ApiResponse<HubTransitRecord>> recordHubScan(@Valid @RequestBody FulfillmentDTOs.HubScanRequest request) {
        HubTransitRecord saved = fulfillmentService.recordHubScan(request);
        return ResponseEntity.ok(ApiResponse.ok(saved, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @GetMapping(ApiPath.HUB_TRANSIT_BY_TRACKING)
    @Operation(summary = "List all hub sorting and transit scans for shipment")
    public ResponseEntity<ApiResponse<List<HubTransitRecord>>> getHubScans(@PathVariable String trackingNumber) {
        List<HubTransitRecord> records = fulfillmentService.getHubScans(trackingNumber);
        return ResponseEntity.ok(ApiResponse.ok(records, messageService.getMessage(MessageCode.SUCCESS)));
    }
}
