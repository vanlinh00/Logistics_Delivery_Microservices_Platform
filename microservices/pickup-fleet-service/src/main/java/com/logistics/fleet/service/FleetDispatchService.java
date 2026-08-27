package com.logistics.fleet.service;

import com.logistics.fleet.constant.KafkaTopic;
import com.logistics.fleet.constant.MessageCode;
import com.logistics.fleet.exception.DriverUnavailableException;
import com.logistics.fleet.exception.ResourceNotFoundException;
import com.logistics.fleet.model.Driver;
import com.logistics.fleet.model.PickupTask;
import com.logistics.fleet.repository.DriverRepository;
import com.logistics.fleet.repository.PickupTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FleetDispatchService {

    private final DriverRepository driverRepository;
    private final PickupTaskRepository pickupTaskRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MessageService messageService;

    private static final Duration DRIVER_STATE_TTL = Duration.ofHours(24);

    @Transactional(readOnly = true)
    public List<Driver> getDrivers(Driver.DriverStatus status) {
        if (status != null) {
            return driverRepository.findByStatus(status);
        }
        return driverRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Driver getDriverById(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage(MessageCode.DRIVER_NOT_FOUND, driverId)));
    }

    @Transactional
    public Driver registerDriver(Driver driver) {
        if (driver.getStatus() == null) {
            driver.setStatus(Driver.DriverStatus.AVAILABLE);
        }
        if (driver.getActiveAssignmentsCount() == null) {
            driver.setActiveAssignmentsCount(0);
        }
        if (driver.getRating() == null) {
            driver.setRating(5.0);
        }
        log.info("Registering new fleet driver [{}] with plate [{}]", driver.getFullName(), driver.getVehiclePlate());
        return driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public List<PickupTask> getPickupTasks(PickupTask.PickupStatus status) {
        if (status != null) {
            return pickupTaskRepository.findByStatus(status);
        }
        return pickupTaskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PickupTask getPickupTaskById(UUID taskId) {
        return pickupTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage(MessageCode.TASK_NOT_FOUND, taskId)));
    }

    @Transactional
    public PickupTask assignDriverToPickup(UUID taskId, UUID driverId) {
        PickupTask task = pickupTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage(MessageCode.TASK_NOT_FOUND, taskId)));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage(MessageCode.DRIVER_NOT_FOUND, driverId)));

        if (driver.getStatus() == Driver.DriverStatus.OFFLINE) {
            throw new DriverUnavailableException(
                    messageService.getMessage(MessageCode.DRIVER_UNAVAILABLE));
        }

        task.setDriverId(driver.getId());
        task.setStatus(PickupTask.PickupStatus.ASSIGNED);
        task.setUpdatedAt(LocalDateTime.now());

        driver.setStatus(Driver.DriverStatus.BUSY);
        driver.setActiveAssignmentsCount((driver.getActiveAssignmentsCount() != null ? driver.getActiveAssignmentsCount() : 0) + 1);
        driverRepository.save(driver);

        // Update driver state in Redis with 24-hour TTL
        try {
            redisTemplate.opsForValue().set("driver:state:" + driver.getId(), "ASSIGNED_PICKUP_" + task.getId(), DRIVER_STATE_TTL);
        } catch (Exception e) {
            log.warn("Failed to update driver state in Redis: {}", e.getMessage());
        }

        // Publish to Kafka topic
        try {
            kafkaTemplate.send(KafkaTopic.FLEET_PICKUP_ASSIGNED, task.getTrackingNumber(), "Driver " + driver.getFullName() + " assigned");
        } catch (Exception e) {
            log.error("Failed to publish to Kafka topic [{}]: {}", KafkaTopic.FLEET_PICKUP_ASSIGNED, e.getMessage());
        }

        return pickupTaskRepository.save(task);
    }

    /**
     * Finds nearest available driver using Euclidean approximation or geospatial index
     */
    @Transactional(readOnly = true)
    public Driver findOptimalDriverForLocation(Double targetLat, Double targetLon, Driver.VehicleType vehicleType) {
        List<Driver> availableDrivers = driverRepository.findByStatusAndVehicleType(
                Driver.DriverStatus.AVAILABLE, vehicleType != null ? vehicleType : Driver.VehicleType.MOTORBIKE
        );

        if (availableDrivers.isEmpty()) {
            return null;
        }

        return availableDrivers.stream()
                .filter(d -> d.getCurrentLatitude() != null && d.getCurrentLongitude() != null)
                .min(Comparator.comparingDouble(d -> {
                    double dLat = d.getCurrentLatitude() - targetLat;
                    double dLon = d.getCurrentLongitude() - targetLon;
                    return Math.sqrt(dLat * dLat + dLon * dLon);
                }))
                .orElse(availableDrivers.get(0));
    }
}
