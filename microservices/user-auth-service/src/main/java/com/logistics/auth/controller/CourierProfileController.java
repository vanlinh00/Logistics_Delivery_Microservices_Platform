package com.logistics.auth.controller;

import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.dto.AuthDTOs.CourierKycRequest;
import com.logistics.auth.dto.AuthDTOs.CourierShiftRequest;
import com.logistics.auth.model.CourierProfile;
import com.logistics.auth.service.CourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/couriers")
@RequiredArgsConstructor
@Tag(name = "Courier KYC & Fleet Profiles", description = "Courier KYC Verification, Vehicle Registration, Shift Toggling, and Hub Fleet Management")
public class CourierProfileController {

    private final CourierService courierService;

    @PostMapping("/kyc")
    @Operation(summary = "Submit or update courier KYC details (Vehicle, CCCD, Hub assignment)")
    @PreAuthorize("hasAnyRole('ROLE_COURIER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CourierProfile>> submitKyc(
            Principal principal,
            @Valid @RequestBody CourierKycRequest request) {
        CourierProfile profile = courierService.submitKyc(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Courier KYC saved"));
    }

    @PutMapping("/shift")
    @Operation(summary = "Toggle courier online/offline shift status")
    @PreAuthorize("hasRole('ROLE_COURIER')")
    public ResponseEntity<ApiResponse<CourierProfile>> toggleShift(
            Principal principal,
            @Valid @RequestBody CourierShiftRequest request) {
        CourierProfile profile = courierService.toggleShift(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Shift status updated"));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get courier profile by user ID")
    public ResponseEntity<ApiResponse<CourierProfile>> getCourierProfile(@PathVariable("userId") UUID userId) {
        CourierProfile profile = courierService.getCourierProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Courier profile retrieved"));
    }

    @GetMapping("/hub/{hubId}/active")
    @Operation(summary = "Get online couriers by Hub ID for dispatch optimization")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DISPATCHER', 'ROLE_HUB_OPERATOR')")
    public ResponseEntity<ApiResponse<List<CourierProfile>>> getActiveCouriersByHub(@PathVariable("hubId") String hubId) {
        List<CourierProfile> couriers = courierService.getActiveCouriersByHub(hubId);
        return ResponseEntity.ok(ApiResponse.ok(couriers, "Active hub couriers retrieved"));
    }
}
