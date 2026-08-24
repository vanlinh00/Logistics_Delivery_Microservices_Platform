package com.logistics.auth.dto;

import com.logistics.auth.model.User;
import lombok.*;

import java.util.UUID;

public class AuthDTOs {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String usernameOrEmail;
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String fullName;
        private String phone;
        private User.UserRole role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenValidationResponse {
        private boolean valid;
        private String username;
        private String role;
        private UUID userId;
    }
}
