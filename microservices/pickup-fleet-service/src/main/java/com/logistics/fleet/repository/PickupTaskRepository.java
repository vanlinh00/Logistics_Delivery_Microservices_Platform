package com.logistics.fleet.repository;

import com.logistics.fleet.model.PickupTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PickupTaskRepository extends JpaRepository<PickupTask, UUID> {
    Optional<PickupTask> findByTrackingNumber(String trackingNumber);
    List<PickupTask> findByDriverIdAndStatus(UUID driverId, PickupTask.PickupStatus status);
    List<PickupTask> findByStatus(PickupTask.PickupStatus status);
}
