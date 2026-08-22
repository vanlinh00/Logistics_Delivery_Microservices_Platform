package com.logistics.fulfillment.controller;

import com.logistics.fulfillment.model.HubTransitRecord;
import com.logistics.fulfillment.model.ProofOfDelivery;
import com.logistics.fulfillment.repository.HubTransitRepository;
import com.logistics.fulfillment.repository.ProofOfDeliveryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fulfillment")
@RequiredArgsConstructor
@Tag(name = "Delivery Fulfillment & Hub Transit", description = "Endpoints for hub package sorting, linehaul transit, and POD submission")
public class FulfillmentController {

    private final ProofOfDeliveryRepository podRepository;
    private final HubTransitRepository transitRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/pod")
    @Operation(summary = "Submit Proof of Delivery (POD) with signature and photo attachment")
    public ResponseEntity<ProofOfDelivery> submitPOD(@RequestBody ProofOfDelivery pod) {
        pod.setResult(ProofOfDelivery.DeliveryResult.SUCCESS);
        ProofOfDelivery saved = podRepository.save(pod);

        // Notify downstream via Kafka
        kafkaTemplate.send("logistics.fulfillment.delivered", pod.getTrackingNumber(), "Delivered to " + pod.getRecipientSignedName());

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/pod/{trackingNumber}")
    @Operation(summary = "Get Proof of Delivery details by tracking number")
    public ResponseEntity<ProofOfDelivery> getPOD(@PathVariable String trackingNumber) {
        return podRepository.findByTrackingNumber(trackingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/hub-transit/scan")
    @Operation(summary = "Scan package checkpoint at logistics hub")
    public ResponseEntity<HubTransitRecord> recordHubScan(@RequestBody HubTransitRecord record) {
        HubTransitRecord saved = transitRepository.save(record);
        kafkaTemplate.send("logistics.fulfillment.hub-scanned", record.getTrackingNumber(), "Scanned at " + record.getSourceHubId());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/hub-transit/{trackingNumber}")
    @Operation(summary = "List all hub sorting and transit scans for shipment")
    public ResponseEntity<List<HubTransitRecord>> getHubScans(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(transitRepository.findByTrackingNumberOrderByScannedAtDesc(trackingNumber));
    }
}
