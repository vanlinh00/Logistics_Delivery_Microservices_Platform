package com.logistics.fleet.controller;

import com.logistics.fleet.dto.ApiResponse;
import com.logistics.fleet.model.Driver;
import com.logistics.fleet.model.PickupTask;
import com.logistics.fleet.repository.DriverRepository;
import com.logistics.fleet.repository.PickupTaskRepository;
import com.logistics.fleet.service.FleetDispatchService;
import com.logistics.fleet.service.ParallelDriverMatchingService;
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
    private final ParallelDriverMatchingService parallelMatchingService;

    @GetMapping("/drivers")
    @Operation(summary = "List all active drivers with status and GPS")
    public ResponseEntity<ApiResponse<List<Driver>>> getDrivers(@RequestParam(required = false) Driver.DriverStatus status) {
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.ok(driverRepository.findByStatus(status), "Filtered drivers"));
        }
        return ResponseEntity.ok(ApiResponse.ok(driverRepository.findAll(), "All drivers"));
    }

    @PostMapping("/drivers")
    @Operation(summary = "Register a new courier driver to the fleet")
    public ResponseEntity<ApiResponse<Driver>> registerDriver(@RequestBody Driver driver) {
        driver.setStatus(Driver.DriverStatus.AVAILABLE);
        return ResponseEntity.ok(ApiResponse.ok(driverRepository.save(driver), "Driver registered successfully"));
    }

    @GetMapping("/pickups")
    @Operation(summary = "List pickup tasks")
    public ResponseEntity<ApiResponse<List<PickupTask>>> getPickups(@RequestParam(required = false) PickupTask.PickupStatus status) {
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.ok(pickupTaskRepository.findByStatus(status), "Filtered pickups"));
        }
        return ResponseEntity.ok(ApiResponse.ok(pickupTaskRepository.findAll(), "All pickups"));
    }

    @PostMapping("/pickups/{taskId}/assign/{driverId}")
    @Operation(summary = "Dispatch pickup task to courier driver")
    public ResponseEntity<ApiResponse<PickupTask>> assignPickup(@PathVariable UUID taskId, @PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok(dispatchService.assignDriverToPickup(taskId, driverId), "Driver assigned"));
    }

    @GetMapping("/find-nearest-driver")
    @Operation(summary = "Auto-find nearest available driver for pickup coordinates")
    public ResponseEntity<ApiResponse<Driver>> findNearestDriver(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false) Driver.VehicleType vehicleType) {
        Driver driver = dispatchService.findOptimalDriverForLocation(lat, lon, vehicleType);
        return driver != null
                ? ResponseEntity.ok(ApiResponse.ok(driver, "Optimal driver found"))
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/rank-drivers-concurrently")
    @Operation(summary = "Multi-threaded parallel calculation ranking all available drivers by Haversine distance and ETA")
    public ResponseEntity<ApiResponse<List<ParallelDriverMatchingService.DriverMatchScore>>> rankDriversConcurrently(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false) Driver.VehicleType vehicleType) {
        List<ParallelDriverMatchingService.DriverMatchScore> ranked = parallelMatchingService.rankDriversConcurrently(lat, lon, vehicleType);
        return ResponseEntity.ok(ApiResponse.ok(ranked, "Calculated " + ranked.size() + " driver match scores across parallel threads"));
    }
}

