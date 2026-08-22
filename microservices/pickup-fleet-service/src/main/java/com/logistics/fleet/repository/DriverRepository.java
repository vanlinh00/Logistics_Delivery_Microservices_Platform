package com.logistics.fleet.repository;

import com.logistics.fleet.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {
    List<Driver> findByStatus(Driver.DriverStatus status);
    List<Driver> findByStatusAndVehicleType(Driver.DriverStatus status, Driver.VehicleType vehicleType);
    List<Driver> findByAssignedHubId(String hubId);
}
