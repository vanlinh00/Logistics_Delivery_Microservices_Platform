package com.logistics.fleet.controller;

import com.logistics.fleet.constant.ApiPath;
import com.logistics.fleet.constant.MessageCode;
import com.logistics.fleet.dto.ApiResponse;
import com.logistics.fleet.model.Driver;
import com.logistics.fleet.model.PickupTask;
import com.logistics.fleet.service.FleetDispatchService;
import com.logistics.fleet.service.MessageService;
import com.logistics.fleet.service.ParallelDriverMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.FLEET_BASE)
@RequiredArgsConstructor
@Tag(name = "Fleet & Pickup Dispatch", description = "Endpoints for driver allocation, batch pickups, and route assignment")
public class FleetController {

    private final FleetDispatchService dispatchService;
    private final ParallelDriverMatchingService parallelMatchingService;
    private final MessageService messageService;

    @GetMapping(ApiPath.DRIVERS)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_MERCHANT')")
    @Operation(summary = "List all active drivers with status and GPS")
    public ResponseEntity<ApiResponse<List<Driver>>> getDrivers(@RequestParam(required = false) Driver.DriverStatus status) {
        List<Driver> drivers = dispatchService.getDrivers(status);
        return ResponseEntity.ok(ApiResponse.ok(drivers, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @PostMapping(ApiPath.DRIVERS)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Register a new courier driver to the fleet")
    public ResponseEntity<ApiResponse<Driver>> registerDriver(@RequestBody Driver driver) {
        Driver registered = dispatchService.registerDriver(driver);
        return new ResponseEntity<>(
                ApiResponse.created(registered, messageService.getMessage(MessageCode.CREATED)),
                HttpStatus.CREATED
        );
    }

    @GetMapping(ApiPath.DRIVERS + "/{driverId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER')")
    @Operation(summary = "Get driver details by ID")
    public ResponseEntity<ApiResponse<Driver>> getDriverById(@PathVariable UUID driverId) {
        Driver driver = dispatchService.getDriverById(driverId);
        return ResponseEntity.ok(ApiResponse.ok(driver, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @GetMapping(ApiPath.PICKUPS)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_MERCHANT')")
    @Operation(summary = "List pickup tasks")
    public ResponseEntity<ApiResponse<List<PickupTask>>> getPickups(@RequestParam(required = false) PickupTask.PickupStatus status) {
        List<PickupTask> pickups = dispatchService.getPickupTasks(status);
        return ResponseEntity.ok(ApiResponse.ok(pickups, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @GetMapping(ApiPath.PICKUPS + "/{taskId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_MERCHANT', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get pickup task details by ID")
    public ResponseEntity<ApiResponse<PickupTask>> getPickupById(@PathVariable UUID taskId) {
        PickupTask task = dispatchService.getPickupTaskById(taskId);
        return ResponseEntity.ok(ApiResponse.ok(task, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @PostMapping(ApiPath.ASSIGN_PICKUP)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER')")
    @Operation(summary = "Dispatch pickup task to courier driver")
    public ResponseEntity<ApiResponse<PickupTask>> assignPickup(@PathVariable UUID taskId, @PathVariable UUID driverId) {
        PickupTask task = dispatchService.assignDriverToPickup(taskId, driverId);
        return ResponseEntity.ok(ApiResponse.ok(task, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @GetMapping(ApiPath.FIND_NEAREST_DRIVER)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_MERCHANT')")
    @Operation(summary = "Auto-find nearest available driver for pickup coordinates")
    public ResponseEntity<ApiResponse<Driver>> findNearestDriver(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false) Driver.VehicleType vehicleType) {
        Driver driver = dispatchService.findOptimalDriverForLocation(lat, lon, vehicleType);
        return ResponseEntity.ok(ApiResponse.ok(driver, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @GetMapping(ApiPath.RANK_DRIVERS_CONCURRENTLY)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_MERCHANT')")
    @Operation(summary = "Multi-threaded parallel calculation ranking all available drivers by Haversine distance and ETA")
    public ResponseEntity<ApiResponse<List<ParallelDriverMatchingService.DriverMatchScore>>> rankDriversConcurrently(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false) Driver.VehicleType vehicleType) {
        List<ParallelDriverMatchingService.DriverMatchScore> ranked = parallelMatchingService.rankDriversConcurrently(lat, lon, vehicleType);
        return ResponseEntity.ok(ApiResponse.ok(ranked, messageService.getMessage(MessageCode.SUCCESS)));
    }
}
