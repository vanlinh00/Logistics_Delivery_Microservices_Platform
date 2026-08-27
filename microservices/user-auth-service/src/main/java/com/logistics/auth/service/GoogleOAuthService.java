package com.logistics.auth.service;

import com.logistics.auth.constant.MessageCode;
import com.logistics.auth.dto.AuthDTOs.AuthResponse;
import com.logistics.auth.exception.AccountInactiveException;
import com.logistics.auth.model.AuthAuditLog;
import com.logistics.auth.model.Permission;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.AuthAuditLogRepository;
import com.logistics.auth.repository.RoleRepository;
import com.logistics.auth.repository.UserRepository;
import com.logistics.auth.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;
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

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();

            if (!Boolean.TRUE.equals(user.getActive())) {
                recordAudit(user.getUsername(), "OAUTH2_LOGIN_FAILED", "Account deactivated: " + normalizedEmail, request);
                throw new AccountInactiveException(messageService.getMessage(MessageCode.ACCOUNT_INACTIVE));
            }

            // Link Google ID if missing
            if (googleSub != null && !googleSub.isBlank() && (user.getGoogleId() == null || !user.getGoogleId().equals(googleSub))) {
                user.setGoogleId(googleSub);
            }

            // Update profile information if not set
            if ((user.getFullName() == null || user.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName);
            }
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && pictureUrl != null && !pictureUrl.isBlank()) {
                user.setAvatarUrl(pictureUrl);
            }

            user.setLastLoginAt(LocalDateTime.now());
            user = userRepository.save(user);
            log.info("Updated existing user {} with Google OAuth info", user.getUsername());
        } else {
            // 2. Create new user with ROLE_CUSTOMER
            String baseUsername = generateBaseUsername(normalizedEmail);
            String uniqueUsername = generateUniqueUsername(baseUsername);

            user = User.builder()
                    .googleId(googleSub)
                    .username(uniqueUsername)
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(fullName != null && !fullName.isBlank() ? fullName : uniqueUsername)
                    .avatarUrl(pictureUrl)
                    .role(User.UserRole.ROLE_CUSTOMER)
                    .active(true)
                    .mfaEnabled(false)
                    .lastLoginAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);
            log.info("Created new user {} via Google OAuth2 with ROLE_CUSTOMER", user.getUsername());
        }

        // 3. Permissions & Token Generation
        List<String> permissions = getPermissionsForRole(user.getRole());
        String accessToken = jwtTokenProvider.generateAccessToken(user, permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000;

        recordAudit(user.getUsername(), "OAUTH2_LOGIN_SUCCESS", "Google login successful for " + normalizedEmail, request);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .permissions(permissions)
                .mfaRequired(false)
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
