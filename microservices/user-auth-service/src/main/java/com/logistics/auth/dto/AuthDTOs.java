package com.logistics.auth.dto;

import com.logistics.auth.model.CourierProfile;
import com.logistics.auth.model.MerchantProfile;
import com.logistics.auth.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AuthDTOs {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        @NotBlank(message = "Username or email must not be blank")
        private String usernameOrEmail;

        @NotBlank(message = "Password must not be blank")
        private String password;

        private String mfaCode; // Optional 6-digit TOTP code if MFA enabled
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
        private String username;

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Full name must not be blank")
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
        private Long expiresIn;
        private UUID userId;
        private String keycloakId;
        private String username;
        private String email;
        private String role;
        private String fullName;
        private List<String> permissions;
        private Boolean mfaRequired;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token must not be blank")
        private String refreshToken;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LogoutRequest {
        private String refreshToken;
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
        private String email;
        private Boolean active;
        private List<String> roles;
        private List<String> permissions;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password must not be blank")
        private String oldPassword;

        @NotBlank(message = "New password must not be blank")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MfaSetupResponse {
        private String secret;
        private String qrCodeUri;
        private String manualEntryKey;
        private List<String> backupCodes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MfaVerifyRequest {
        @NotBlank(message = "TOTP authentication code must not be blank")
        private String code;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourierKycRequest {
        @NotBlank(message = "Citizen ID must not be blank")
        private String citizenId;

        private CourierProfile.VehicleType vehicleType;

        @NotBlank(message = "License plate must not be blank")
        private String licensePlate;

        @NotBlank(message = "Assigned Hub ID must not be blank")
        private String assignedHubId;

        private Double maxCapacityKg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourierShiftRequest {
        private Boolean isOnline;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MerchantProfileRequest {
        @NotBlank(message = "Shop name must not be blank")
        private String shopName;

        private String taxCode;

        @NotBlank(message = "Warehouse address must not be blank")
        private String warehouseAddress;

        private String bankAccount;
        private String bankName;
        private MerchantProfile.CodTier codTier;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummaryDTO {
        private UUID id;
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private String role;
        private Boolean active;
        private Boolean mfaEnabled;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private CourierProfile courierProfile;
        private MerchantProfile merchantProfile;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthStatsResponse {
        private long totalUsers;
        private long activeUsers;
        private long totalCouriers;
        private long totalMerchants;
        private long totalCustomers;
        private long activeCouriersOnline;
    }
}
