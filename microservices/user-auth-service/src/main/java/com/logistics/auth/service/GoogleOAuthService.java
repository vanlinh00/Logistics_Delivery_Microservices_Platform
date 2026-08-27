package com.logistics.auth.service;

import com.logistics.auth.constant.MessageCode;
import com.logistics.auth.dto.AuthDTOs.AuthResponse;
import com.logistics.auth.exception.AccountInactiveException;
import com.logistics.auth.model.AuthAuditLog;
import com.logistics.auth.model.CourierProfile;
import com.logistics.auth.model.MerchantProfile;
import com.logistics.auth.model.Permission;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.AuthAuditLogRepository;
import com.logistics.auth.repository.CourierProfileRepository;
import com.logistics.auth.repository.MerchantProfileRepository;
import com.logistics.auth.repository.RoleRepository;
import com.logistics.auth.repository.UserRepository;
import com.logistics.auth.security.KeycloakClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 🚀 GoogleOAuthService:
 * Handles user synchronization, account linking, security validation, and token generation
 * for Google OAuth 2.0 logins.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final KeycloakClient keycloakClient;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditLogRepository auditLogRepository;
    private final MessageService messageService;

    @Transactional
    public AuthResponse processGoogleUser(String googleSub, String email, String fullName, String pictureUrl, HttpServletRequest request) {
        log.info("Processing Google OAuth2 authentication for sub={}, email={}", googleSub, email);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account did not provide a valid email address");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Find user by Google ID or Email
        Optional<User> existingUserOpt = (googleSub != null && !googleSub.isBlank())
                ? userRepository.findByGoogleId(googleSub).or(() -> userRepository.findByEmail(normalizedEmail))
                : userRepository.findByEmail(normalizedEmail);

        User savedUser;
        String rawPassword = null;
        if (existingUserOpt.isPresent()) {
            savedUser = existingUserOpt.get();

            if (!Boolean.TRUE.equals(savedUser.getActive())) {
                recordAudit(savedUser.getUsername(), "OAUTH2_LOGIN_FAILED", "Account deactivated: " + normalizedEmail, request);
                throw new AccountInactiveException(messageService.getMessage(MessageCode.ACCOUNT_INACTIVE));
            }

            // Link Google ID if missing
            if (googleSub != null && !googleSub.isBlank() && (savedUser.getGoogleId() == null || !savedUser.getGoogleId().equals(googleSub))) {
                savedUser.setGoogleId(googleSub);
            }

            // Update profile information if not set
            if ((savedUser.getFullName() == null || savedUser.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
                savedUser.setFullName(fullName);
            }
            if ((savedUser.getAvatarUrl() == null || savedUser.getAvatarUrl().isBlank()) && pictureUrl != null && !pictureUrl.isBlank()) {
                savedUser.setAvatarUrl(pictureUrl);
            }

            savedUser.setLastLoginAt(LocalDateTime.now());
            savedUser = userRepository.save(savedUser);
            log.info("Updated existing user {} with Google OAuth info", savedUser.getUsername());
        } else {
            // 2. Create new user with ROLE_CUSTOMER
            String baseUsername = generateBaseUsername(normalizedEmail);
            String uniqueUsername = generateUniqueUsername(baseUsername);
            rawPassword = UUID.randomUUID().toString();

            User.UserRole role = User.UserRole.ROLE_CUSTOMER;

            User user = User.builder()
                    .googleId(googleSub)
                    .username(uniqueUsername)
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName != null && !fullName.isBlank() ? fullName : uniqueUsername)
                    .avatarUrl(pictureUrl)
                    .role(role)
                    .active(true)
                    .mfaEnabled(false)
                    .lastLoginAt(LocalDateTime.now())
                    .build();

            savedUser = userRepository.save(user);

            // Provision user in Keycloak IAM
            Optional<String> kcUserId = keycloakClient.createUser(
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    rawPassword,
                    savedUser.getFullName(),
                    role.name()
            );
            if (kcUserId.isPresent()) {
                savedUser.setKeycloakId(kcUserId.get());
                savedUser = userRepository.save(savedUser);
            }

            // Auto-create role-specific profile records
            if (role == User.UserRole.ROLE_COURIER) {
                CourierProfile courier = CourierProfile.builder()
                        .user(savedUser)
                        .vehicleType(CourierProfile.VehicleType.MOTORBIKE)
                        .kycStatus(CourierProfile.KycStatus.PENDING)
                        .isOnline(false)
                        .rating(5.0)
                        .totalDeliveries(0)
                        .assignedHubId("HUB-DEFAULT-01")
                        .build();
                courierProfileRepository.save(courier);
            } else if (role == User.UserRole.ROLE_MERCHANT) {
                MerchantProfile merchant = MerchantProfile.builder()
                        .user(savedUser)
                        .shopName(savedUser.getFullName() != null ? savedUser.getFullName() + " Shop" : "My Shop")
                        .codTier(MerchantProfile.CodTier.STANDARD)
                        .discountRate(0.05)
                        .build();
                merchantProfileRepository.save(merchant);
            }

            log.info("Created new user {} via Google OAuth2 with ROLE_CUSTOMER", savedUser.getUsername());
        }

        recordAudit(savedUser.getUsername(), "REGISTER_OAUTH2", "Google OAuth user processed: " + savedUser.getRole(), request);

        // Auto-login via Keycloak
        Optional<KeycloakClient.KeycloakTokenResponse> kcToken = rawPassword != null
                ? keycloakClient.login(savedUser.getUsername(), rawPassword)
                : Optional.empty();
        List<String> userPermissions = getPermissionsForRole(savedUser.getRole());

        String accessToken = kcToken.map(KeycloakClient.KeycloakTokenResponse::accessToken).orElse("");
        String refreshToken = kcToken.map(KeycloakClient.KeycloakTokenResponse::refreshToken).orElse("");
        long expiresIn = kcToken.map(KeycloakClient.KeycloakTokenResponse::expiresIn).orElse(86400L);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .fullName(savedUser.getFullName())
                .permissions(userPermissions)
                .message(messageService.getMessage(MessageCode.SUCCESS))
                .build();
    }

    public List<String> getPermissionsForRole(User.UserRole role) {
        if (role == null) {
            return Collections.emptyList();
        }
        return roleRepository.findByCodeWithPermissions(role.name())
                .map(r -> r.getPermissions() != null
                        ? r.getPermissions().stream().map(Permission::getCode).sorted().toList()
                        : Collections.<String>emptyList())
                .filter(list -> !list.isEmpty())
                .orElseGet(() -> switch (role) {
                    case ROLE_ADMIN -> List.of("orders:create", "orders:read", "orders:update", "orders:cancel", "orders:delete", "fleet:view", "fleet:dispatch", "users:read", "users:write", "analytics:view");
                    case ROLE_MERCHANT -> List.of("orders:create", "orders:read", "orders:cancel", "analytics:view");
                    case ROLE_COURIER -> List.of("orders:read", "orders:status:update", "fleet:status:update");
                    case ROLE_DISPATCHER -> List.of("orders:read", "orders:update", "fleet:dispatch", "fleet:view");
                    default -> List.of("orders:create", "orders:read", "orders:cancel");
                });
    }

    private String generateBaseUsername(String email) {
        String prefix = email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
        if (prefix.length() < 3) {
            prefix = "user_" + prefix;
        }
        if (prefix.length() > 30) {
            prefix = prefix.substring(0, 30);
        }
        return prefix;
    }

    private String generateUniqueUsername(String baseUsername) {
        if (!userRepository.existsByUsername(baseUsername)) {
            return baseUsername;
        }
        for (int i = 1; i <= 100; i++) {
            String candidate = baseUsername + "_" + i;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        return baseUsername + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private void recordAudit(String username, String eventType, String details, HttpServletRequest request) {
        try {
            String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
            String userAgent = request != null ? request.getHeader("User-Agent") : "Internal-OAuth2-Provider";

            AuthAuditLog logEntry = AuthAuditLog.builder()
                    .username(username)
                    .eventType(eventType)
                    .ipAddress(ip)
                    .userAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to record auth audit log: {}", e.getMessage());
        }
    }
}
