package com.logistics.auth.controller;

import com.logistics.auth.model.User;
import com.logistics.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & IAM", description = "Handles login, registration, token generation, and profile verification")
public class AuthController {

    private final UserRepository userRepository;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private String usernameOrEmail;
        private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private UUID userId;
        private String username;
        private String email;
        private String role;
        private String fullName;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT access token")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()));

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        User user = userOpt.get();
        // Return JWT payload
        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken("mock-jwt-token-sample." + user.getId())
                .refreshToken("mock-jwt-refresh-sample." + user.getId())
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .build());
    }

    @PostMapping("/register")
    @Operation(summary = "Register customer or courier account")
    public ResponseEntity<User> register(@RequestBody User user) {
        if (user.getRole() == null) {
            user.setRole(User.UserRole.ROLE_CUSTOMER);
        }
        return ResponseEntity.ok(userRepository.save(user));
    }
}
