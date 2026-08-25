package com.logistics.auth.controller;

import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.dto.AuthDTOs.MerchantProfileRequest;
import com.logistics.auth.model.MerchantProfile;
import com.logistics.auth.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant Profile & Settings", description = "Merchant Shop Information, Tax IDs, Warehouses, and COD Tiers")
public class MerchantProfileController {

    private final MerchantService merchantService;

    @PostMapping("/profile")
    @Operation(summary = "Save or update merchant business profile, warehouse, and bank account")
    @PreAuthorize("hasAnyRole('ROLE_MERCHANT', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MerchantProfile>> saveMerchantProfile(
            Principal principal,
            @Valid @RequestBody MerchantProfileRequest request) {
        MerchantProfile profile = merchantService.saveMerchantProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Merchant profile updated"));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get merchant profile by user ID")
    public ResponseEntity<ApiResponse<MerchantProfile>> getMerchantProfile(@PathVariable("userId") UUID userId) {
        MerchantProfile profile = merchantService.getMerchantProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Merchant profile retrieved"));
    }
}
