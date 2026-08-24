package com.logistics.auth.controller;

import com.logistics.auth.dto.AuthDTOs.*;
import com.logistics.auth.model.User;
import com.logistics.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register customer or courier account")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT Token and return principal role/userId")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam("token") String token) {
        TokenValidationResponse validation = authService.validateToken(token);
        return ResponseEntity.ok(validation);
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile details by User UUID")
    public ResponseEntity<User> getProfile(@PathVariable("userId") UUID userId) {
        User user = authService.getUserProfile(userId);
        return ResponseEntity.ok(user);
    }
}

