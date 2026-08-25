package com.logistics.auth.controller;

import com.logistics.auth.constant.ApiPath;
import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.dto.AuthDTOs.*;
import com.logistics.auth.model.User;
import com.logistics.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.USERS_BASE)
@RequiredArgsConstructor
@Tag(name = "User Management & Administration", description = "User Profiles, Passwords, and Admin User Governance")
public class UserController {

    private final UserService userService;

    @GetMapping(ApiPath.BY_ID)
    @Operation(summary = "Get user entity by ID")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId.toString() == authentication.principal")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable("userId") UUID userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.ok(user, "User retrieved"));
    }

    @PostMapping(ApiPath.PASSWORD_CHANGE)
    @Operation(summary = "Change current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }

    @GetMapping(ApiPath.ADMIN_ALL)
    @Operation(summary = "List all platform users (Admin only)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(Pageable pageable) {
        Page<User> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.ok(users, "User page retrieved"));
    }

    @PutMapping(ApiPath.ADMIN_STATUS)
    @Operation(summary = "Activate or Deactivate user account (Admin only)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<User>> toggleStatus(
            @PathVariable("userId") UUID userId,
            @RequestParam("active") boolean active) {
        User updated = userService.toggleUserStatus(userId, active);
        return ResponseEntity.ok(ApiResponse.ok(updated, "User status updated"));
    }

    @GetMapping(ApiPath.STATS)
    @Operation(summary = "Get platform IAM and user statistics")
    public ResponseEntity<ApiResponse<AuthStatsResponse>> getStats() {
        AuthStatsResponse stats = userService.getStats();
        return ResponseEntity.ok(ApiResponse.ok(stats, "IAM stats retrieved"));
    }
}
