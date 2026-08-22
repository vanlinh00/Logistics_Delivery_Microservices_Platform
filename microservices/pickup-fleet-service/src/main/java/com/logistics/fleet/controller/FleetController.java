package com.logistics.fleet.controller;

import com.logistics.fleet.model.Driver;
import com.logistics.fleet.model.PickupTask;
import com.logistics.fleet.repository.DriverRepository;
import com.logistics.fleet.repository.PickupTaskRepository;
import com.logistics.fleet.service.FleetDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fleet")
@RequiredArgsConstructor
@Tag(name = "Fleet & Pickup Dispatch", description = "Endpoints for driver allocation, batch pickups, and route assignment")
public class FleetController {

    private final DriverRepository driverRepository;
    private final PickupTaskRepository pickupTaskRepository;
    private final FleetDispatchService dispatchService;

    @GetMapping("/drivers")
    @Operation(summary = "List all active drivers with status and GPS")
    public ResponseEntity<List<Driver>> getDrivers(@RequestParam(required = false) Driver.DriverStatus status) {
        if (status != null) {
            return ResponseEntity.ok(driverRepository.findByStatus(status));
        }
        return ResponseEntity.ok(driverRepository.findAll());
    }

    @PostMapping("/drivers")
    @Operation(summary = "Register a new courier driver to the fleet")
    public ResponseEntity<Driver> registerDriver(@RequestBody Driver driver) {
        driver.setStatus(Driver.DriverStatus.AVAILABLE);
        return ResponseEntity.ok(driverRepository.save(driver));
    }

    @GetMapping("/pickups")
    @Operation(summary = "List pickup tasks")
    public ResponseEntity<List<PickupTask>> getPickups(@RequestParam(required = false) PickupTask.PickupStatus status) {
        if (status != null) {
            return ResponseEntity.ok(pickupTaskRepository.findByStatus(status));
        }
        return ResponseEntity.ok(pickupTaskRepository.findAll());
    }

    @PostMapping("/pickups/{taskId}/assign/{driverId}")
    @Operation(summary = "Dispatch pickup task to courier driver")
    public ResponseEntity<PickupTask> assignPickup(@PathVariable UUID taskId, @PathVariable UUID driverId) {
        return ResponseEntity.ok(dispatchService.assignDriverToPickup(taskId, driverId));
    }

    @GetMapping("/find-nearest-driver")
    @Operation(summary = "Auto-find nearest available driver for pickup coordinates")
    public ResponseEntity<Driver> findNearestDriver(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false) Driver.VehicleType vehicleType) {
        Driver driver = dispatchService.findOptimalDriverForLocation(lat, lon, vehicleType);
        return driver != null ? ResponseEntity.ok(driver) : ResponseEntity.notFound().build();
    }
}
