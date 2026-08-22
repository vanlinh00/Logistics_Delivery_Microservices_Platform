package com.logistics.fleet.service;

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

    @Transactional
    public PickupTask assignDriverToPickup(UUID taskId, UUID driverId) {
        PickupTask task = pickupTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        task.setDriverId(driver.getId());
        task.setStatus(PickupTask.PickupStatus.ASSIGNED);
        task.setUpdatedAt(LocalDateTime.now());

        // Update driver state in Redis
        redisTemplate.opsForValue().set("driver:state:" + driver.getId(), "ASSIGNED_PICKUP_" + task.getId());

        // Publish to Kafka topic
        kafkaTemplate.send("logistics.fleet.pickup-assigned", task.getTrackingNumber(), "Driver " + driver.getFullName() + " assigned");

        return pickupTaskRepository.save(task);
    }

    /**
     * Finds nearest available driver using Euclidean approximation or geospatial index
     */
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
