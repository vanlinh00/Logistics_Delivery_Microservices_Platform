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
        @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
        private String usernameOrEmail;

        @NotBlank(message = "Mật khẩu không được để trống")
        private String password;

        private String mfaCode; // Optional 6-digit TOTP code if MFA enabled
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(min = 3, max = 32, message = "Tên đăng nhập từ 3 đến 32 ký tự")
        private String username;

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Định dạng email không hợp lệ")
        private String email;

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
        private String password;

        @NotBlank(message = "Họ và tên không được để trống")
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
        @NotBlank(message = "Refresh token không được để trống")
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
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangePasswordRequest {
        @NotBlank(message = "Mật khẩu cũ không được để trống")
        private String oldPassword;

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, message = "Mật khẩu mới tối thiểu 6 ký tự")
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
        @NotBlank(message = "Mã xác thực TOTP không được để trống")
        private String code;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourierKycRequest {
        @NotBlank(message = "Số CCCD / CMND không được để trống")
        private String citizenId;

        private CourierProfile.VehicleType vehicleType;

        @NotBlank(message = "Biển số xe không được để trống")
        private String licensePlate;

        @NotBlank(message = "Mã Hub điều phối không được để trống")
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
        @NotBlank(message = "Tên cửa hàng/doanh nghiệp không được để trống")
        private String shopName;

        private String taxCode;

        @NotBlank(message = "Địa chỉ kho hàng không được để trống")
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
