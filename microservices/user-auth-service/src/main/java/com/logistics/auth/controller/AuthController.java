package com.logistics.auth.controller;

import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.dto.AuthDTOs.*;
import com.logistics.auth.model.User;
import com.logistics.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & IAM", description = "Handles login, registration, token generation, and profile verification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register customer or courier account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "User registered successfully"));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT Token and return principal role/userId")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(@RequestParam("token") String token) {
        TokenValidationResponse validation = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.ok(validation, "Token validated"));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile details by User UUID")
    public ResponseEntity<ApiResponse<User>> getProfile(@PathVariable("userId") UUID userId) {
        User user = authService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(user, "Profile retrieved"));
    }
}


