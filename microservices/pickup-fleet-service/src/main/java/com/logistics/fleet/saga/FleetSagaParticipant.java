package com.logistics.fleet.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.fleet.model.CourierDriver;
import com.logistics.fleet.model.PickupTask;
import com.logistics.fleet.repository.CourierDriverRepository;
import com.logistics.fleet.repository.PickupTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saga Participant for Fleet Management Service.
 * Listens for Saga Commands and publishes Execution / Compensation Results.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FleetSagaParticipant {

    private final PickupTaskRepository pickupTaskRepository;
    private final CourierDriverRepository driverRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_FLEET_RESULTS = "logistics.fleet.pickup-results";

    @KafkaListener(topics = "logistics.fleet.commands", groupId = "fleet-saga-group")
    @Transactional
    public void handleFleetCommand(String message) {
        try {
            Map<String, Object> commandMap = objectMapper.readValue(message, Map.class);
            log.info("🚚 [FLEET SAGA PARTICIPANT] Received command: {}", commandMap);

            if (commandMap.containsKey("senderAddress")) {
                // Execute Forward Step: Dispatch Driver for Pickup
                processFleetPickupCommand(commandMap);
            } else if (commandMap.containsKey("reason")) {
                // Execute Compensation Step: Rollback/Cancel Driver Allocation
                processFleetCompensation(commandMap);
            }
        } catch (Exception e) {
            log.error("Failed to process fleet saga command: {}", e.getMessage());
        }
    }

    private void processFleetPickupCommand(Map<String, Object> cmd) {
        UUID orderId = UUID.fromString((String) cmd.get("orderId"));
        String trackingNumber = (String) cmd.get("trackingNumber");
        String address = (String) cmd.get("senderAddress");

        List<CourierDriver> availableDrivers = driverRepository.findByStatus(CourierDriver.DriverStatus.AVAILABLE);

        if (availableDrivers.isEmpty()) {
            log.warn("⚠️ No available courier drivers found for order {}", trackingNumber);
            publishResult(orderId, trackingNumber, false, null, "No courier drivers currently available in area");
            return;
        }

        // Assign first available driver
        CourierDriver driver = availableDrivers.get(0);
        driver.setStatus(CourierDriver.DriverStatus.BUSY);
        driver.setCurrentTasksCount(driver.getCurrentTasksCount() + 1);
        driverRepository.save(driver);

        PickupTask task = PickupTask.builder()
                .trackingNumber(trackingNumber)
                .pickupAddress(address)
                .status(PickupTask.PickupStatus.ASSIGNED)
                .assignedDriverId(driver.getId().toString())
                .driverName(driver.getFullName())
                .driverPhone(driver.getPhone())
                .scheduledTime(LocalDateTime.now().plusHours(1))
                .build();
        pickupTaskRepository.save(task);

        log.info("✅ Assigned Driver [{}] to Order [{}]", driver.getFullName(), trackingNumber);
        publishResult(orderId, trackingNumber, true, driver.getId().toString(), null);
    }

    private void processFleetCompensation(Map<String, Object> cmd) {
        String trackingNumber = (String) cmd.get("trackingNumber");
        String reason = (String) cmd.get("reason");
        log.warn("🔄 [FLEET SAGA ROLLBACK] Cancelling driver task for Tracking {}. Reason: {}", trackingNumber, reason);

        List<PickupTask> tasks = pickupTaskRepository.findAll();
        tasks.stream()
                .filter(t -> trackingNumber.equals(t.getTrackingNumber()))
                .findFirst()
                .ifPresent(task -> {
                    task.setStatus(PickupTask.PickupStatus.CANCELLED);
                    pickupTaskRepository.save(task);

                    if (task.getAssignedDriverId() != null) {
                        try {
                            UUID driverId = UUID.fromString(task.getAssignedDriverId());
                            driverRepository.findById(driverId).ifPresent(driver -> {
                                driver.setStatus(CourierDriver.DriverStatus.AVAILABLE);
                                driver.setCurrentTasksCount(Math.max(0, driver.getCurrentTasksCount() - 1));
                                driverRepository.save(driver);
                                log.info("Driver [{}] released back to AVAILABLE pool.", driver.getFullName());
                            });
                        } catch (Exception ignored) {}
                    }
                });
    }

    private void publishResult(UUID orderId, String trackingNumber, boolean success, String driverId, String error) {
        try {
            Map<String, Object> result = Map.of(
                    "orderId", orderId.toString(),
                    "trackingNumber", trackingNumber,
                    "success", success,
                    "assignedDriverId", driverId != null ? driverId : "",
                    "failureReason", error != null ? error : ""
            );
            kafkaTemplate.send(TOPIC_FLEET_RESULTS, orderId.toString(), objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Failed to publish fleet saga result: {}", e.getMessage());
        }
    }
}
