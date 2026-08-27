package com.logistics.fulfillment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.fulfillment.constant.KafkaTopic;
import com.logistics.fulfillment.constant.MessageCode;
import com.logistics.fulfillment.dto.FulfillmentDTOs;
import com.logistics.fulfillment.exception.ResourceNotFoundException;
import com.logistics.fulfillment.model.HubTransitRecord;
import com.logistics.fulfillment.model.ProofOfDelivery;
import com.logistics.fulfillment.repository.HubTransitRepository;
import com.logistics.fulfillment.repository.ProofOfDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 📦 FulfillmentService:
 * Core business service handling proof of delivery submissions, sorting hub scanning,
 * linehaul transit updates, and downstream Kafka telemetry publishing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FulfillmentService {

    private final ProofOfDeliveryRepository podRepository;
    private final HubTransitRepository transitRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Submit Proof of Delivery (POD) record and publish fulfillment event.
     */
    @Transactional
    public ProofOfDelivery submitProofOfDelivery(ProofOfDelivery pod) {
        if (pod.getTrackingNumber() == null || pod.getTrackingNumber().isBlank()) {
            throw new IllegalArgumentException("Tracking number must not be blank");
        }

        if (pod.getOrderId() == null) {
            pod.setOrderId(UUID.randomUUID());
        }

        if (pod.getResult() == null) {
            pod.setResult(ProofOfDelivery.DeliveryResult.SUCCESS);
        }

        if (pod.getDeliveredAt() == null) {
            pod.setDeliveredAt(LocalDateTime.now());
        }

        // Idempotency: Check if POD exists for tracking number
        ProofOfDelivery saved = podRepository.findByTrackingNumber(pod.getTrackingNumber())
                .map(existing -> {
                    existing.setRecipientSignedName(pod.getRecipientSignedName());
                    existing.setRecipientPhone(pod.getRecipientPhone());
                    existing.setSignatureDataUri(pod.getSignatureDataUri());
                    existing.setPhotoEvidenceUrl(pod.getPhotoEvidenceUrl());
                    existing.setDeliveryLatitude(pod.getDeliveryLatitude());
                    existing.setDeliveryLongitude(pod.getDeliveryLongitude());
                    existing.setCourierId(pod.getCourierId());
                    existing.setCourierNotes(pod.getCourierNotes());
                    existing.setResult(pod.getResult());
                    existing.setDeliveredAt(LocalDateTime.now());
                    return podRepository.save(existing);
                })
                .orElseGet(() -> podRepository.save(pod));

        log.info("Proof of Delivery submitted for trackingNumber: [{}], recipient: [{}]",
                saved.getTrackingNumber(), saved.getRecipientSignedName());

        // Publish fulfillment delivered event to Kafka
        publishDeliveryEvent(saved);

        return saved;
    }

    /**
     * Submit POD via DTO request.
     */
    @Transactional
    public ProofOfDelivery submitProofOfDelivery(FulfillmentDTOs.PodSubmitRequest request) {
        ProofOfDelivery pod = ProofOfDelivery.builder()
                .trackingNumber(request.getTrackingNumber())
                .orderId(request.getOrderId() != null ? request.getOrderId() : UUID.randomUUID())
                .recipientSignedName(request.getRecipientSignedName())
                .recipientPhone(request.getRecipientPhone())
                .signatureDataUri(request.getSignatureDataUri())
                .photoEvidenceUrl(request.getPhotoEvidenceUrl())
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .courierId(request.getCourierId())
                .courierNotes(request.getCourierNotes())
                .result(request.getResult() != null ? request.getResult() : ProofOfDelivery.DeliveryResult.SUCCESS)
                .deliveredAt(LocalDateTime.now())
                .build();

        return submitProofOfDelivery(pod);
    }

    /**
     * Get Proof of Delivery details by tracking number.
     */
    @Transactional(readOnly = true)
    public ProofOfDelivery getProofOfDelivery(String trackingNumber) {
        return podRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageCode.POD_NOT_FOUND,
                        messageService.getMessage(MessageCode.POD_NOT_FOUND, trackingNumber)
                ));
    }

    /**
     * Scan package checkpoint at logistics hub / cross-dock and notify downstream consumers.
     */
    @Transactional
    public HubTransitRecord recordHubScan(HubTransitRecord record) {
        if (record.getTrackingNumber() == null || record.getTrackingNumber().isBlank()) {
            throw new IllegalArgumentException("Tracking number must not be blank");
        }
        if (record.getSourceHubId() == null || record.getSourceHubId().isBlank()) {
            throw new IllegalArgumentException("Source hub ID must not be blank");
        }
        if (record.getDestinationHubId() == null || record.getDestinationHubId().isBlank()) {
            record.setDestinationHubId("HUB-DEST-DEFAULT");
        }
        if (record.getStatus() == null) {
            record.setStatus(HubTransitRecord.TransitStatus.SORTED_AT_ORIGIN);
        }
        if (record.getScannedAt() == null) {
            record.setScannedAt(LocalDateTime.now());
        }

        HubTransitRecord saved = transitRepository.save(record);
        log.info("Recorded Hub Scan for trackingNumber: [{}], sourceHub: [{}], status: [{}]",
                saved.getTrackingNumber(), saved.getSourceHubId(), saved.getStatus());

        // Publish Hub Scanned event to Kafka
        publishHubScanEvent(saved);

        return saved;
    }

    /**
     * Record Hub Scan via DTO request.
     */
    @Transactional
    public HubTransitRecord recordHubScan(FulfillmentDTOs.HubScanRequest request) {
        HubTransitRecord record = HubTransitRecord.builder()
                .trackingNumber(request.getTrackingNumber())
                .sourceHubId(request.getSourceHubId())
                .destinationHubId(request.getDestinationHubId() != null ? request.getDestinationHubId() : "HUB-DEST-DEFAULT")
                .vehiclePlate(request.getVehiclePlate())
                .containerSealNumber(request.getContainerSealNumber())
                .status(request.getStatus() != null ? request.getStatus() : HubTransitRecord.TransitStatus.SORTED_AT_ORIGIN)
                .scannedAt(LocalDateTime.now())
                .build();

        return recordHubScan(record);
    }

    /**
     * List all chronological hub transit records for a tracking number.
     */
    @Transactional(readOnly = true)
    public List<HubTransitRecord> getHubScans(String trackingNumber) {
        return transitRepository.findByTrackingNumberOrderByScannedAtDesc(trackingNumber);
    }

    private void publishDeliveryEvent(ProofOfDelivery pod) {
        try {
            String payload = objectMapper.writeValueAsString(pod);
            String topic = (pod.getResult() == ProofOfDelivery.DeliveryResult.SUCCESS)
                    ? KafkaTopic.FULFILLMENT_DELIVERED
                    : KafkaTopic.FULFILLMENT_FAILED;

            kafkaTemplate.send(topic, pod.getTrackingNumber(), payload);
            log.debug("Published POD event to Kafka topic [{}]: {}", topic, payload);
        } catch (Exception ex) {
            log.error("Failed to publish POD Kafka event for tracking [{}]: {}", pod.getTrackingNumber(), ex.getMessage());
        }
    }

    private void publishHubScanEvent(HubTransitRecord record) {
        try {
            String payload = objectMapper.writeValueAsString(record);
            kafkaTemplate.send(KafkaTopic.FULFILLMENT_HUB_SCANNED, record.getTrackingNumber(), payload);
            log.debug("Published Hub Scan event to Kafka topic [{}]: {}", KafkaTopic.FULFILLMENT_HUB_SCANNED, payload);
        } catch (Exception ex) {
            log.error("Failed to publish Hub Scan Kafka event for tracking [{}]: {}", record.getTrackingNumber(), ex.getMessage());
        }
    }
}
