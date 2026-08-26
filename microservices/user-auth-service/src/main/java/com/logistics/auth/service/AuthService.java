package com.logistics.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.auth.constant.MessageCode;
import com.logistics.auth.dto.AuthDTOs.*;
import com.logistics.auth.exception.AccountInactiveException;
import com.logistics.auth.exception.DuplicateUserException;
import com.logistics.auth.exception.InvalidCredentialsException;
import com.logistics.auth.exception.ResourceNotFoundException;
import com.logistics.auth.model.AuthAuditLog;
import com.logistics.auth.model.CourierProfile;
import com.logistics.auth.model.MerchantProfile;
import com.logistics.auth.model.Permission;
import com.logistics.auth.model.Role;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.AuthAuditLogRepository;
import com.logistics.auth.repository.CourierProfileRepository;
import com.logistics.auth.repository.MerchantProfileRepository;
import com.logistics.auth.repository.RoleRepository;
import com.logistics.auth.repository.UserRepository;
import com.logistics.auth.security.KeycloakClient;
import com.logistics.auth.security.TokenBlacklistService;
import com.logistics.auth.security.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final AuthAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakClient keycloakClient;
    private final TokenBlacklistService blacklistService;
    private final TotpService totpService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.getUsernameOrEmail();
        log.info("Processing authentication for: {}", identifier);

        // 1. Check local user database
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> {
                    recordAudit(identifier, "LOGIN_FAILED", "User not found", httpRequest);
                    return new InvalidCredentialsException(messageService.getMessage(MessageCode.UNAUTHORIZED));
                });

        if (!Boolean.TRUE.equals(user.getActive())) {
            recordAudit(identifier, "LOGIN_FAILED", "Account deactivated", httpRequest);
            throw new AccountInactiveException(messageService.getMessage(MessageCode.ACCOUNT_INACTIVE));
        }

        // 2. Validate password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash())
                || request.getPassword().equals(user.getPasswordHash());

        if (!passwordMatches) {
            recordAudit(identifier, "LOGIN_FAILED", "Invalid password", httpRequest);
            throw new InvalidCredentialsException(messageService.getMessage(MessageCode.UNAUTHORIZED));
        }

        // 3. MFA verification check if 2FA is enabled for this account
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            if (request.getMfaCode() == null || request.getMfaCode().isBlank()) {
                return AuthResponse.builder()
                        .mfaRequired(true)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .message(messageService.getMessage(MessageCode.MFA_REQUIRED))
                        .build();
            }

            boolean validTotp = totpService.verifyCode(user.getMfaSecret(), request.getMfaCode());
            if (!validTotp) {
                recordAudit(identifier, "MFA_FAILED", "Invalid TOTP code", httpRequest);
                throw new InvalidCredentialsException(messageService.getMessage(MessageCode.MFA_INVALID));
            }
        }

        // 4. Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 5. Authenticate via Keycloak Direct Grant
        Optional<KeycloakClient.KeycloakTokenResponse> kcToken = keycloakClient.login(user.getUsername(), request.getPassword());

        String accessToken = kcToken.map(KeycloakClient.KeycloakTokenResponse::accessToken).orElse(null);
        String refreshToken = kcToken.map(KeycloakClient.KeycloakTokenResponse::refreshToken).orElse(null);
        long expiresIn = kcToken.map(KeycloakClient.KeycloakTokenResponse::expiresIn).orElse(3600L);

        List<String> userPermissions = getPermissionsForRole(user.getRole());
        recordAudit(user.getUsername(), "LOGIN_SUCCESS", "Role: " + user.getRole(), httpRequest);

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
                .permissions(userPermissions)
                .mfaRequired(false)
                .message(messageService.getMessage(MessageCode.SUCCESS))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registering new user: {} (role: {})", request.getUsername(), request.getRole());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already registered: " + request.getEmail());
        }

        User.UserRole role = request.getRole() != null ? request.getRole() : User.UserRole.ROLE_CUSTOMER;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(role)
                .active(true)
                .mfaEnabled(false)
                .build();

        User savedUser = userRepository.save(user);

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

        recordAudit(savedUser.getUsername(), "REGISTER", "New user registered: " + role, httpRequest);

        // Auto-login via Keycloak if available
        Optional<KeycloakClient.KeycloakTokenResponse> kcToken = keycloakClient.login(savedUser.getUsername(), request.getPassword());

        List<String> userPermissions = getPermissionsForRole(savedUser.getRole());

        return AuthResponse.builder()
                .accessToken(kcToken.map(KeycloakClient.KeycloakTokenResponse::accessToken).orElse(null))
                .refreshToken(kcToken.map(KeycloakClient.KeycloakTokenResponse::refreshToken).orElse(null))
                .tokenType("Bearer")
                .expiresIn(kcToken.map(KeycloakClient.KeycloakTokenResponse::expiresIn).orElse(3600L))
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .fullName(savedUser.getFullName())
                .permissions(userPermissions)
                .message(messageService.getMessage(MessageCode.CREATED))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException(messageService.getMessage(MessageCode.TOKEN_INVALID));
        }

        Optional<KeycloakClient.KeycloakTokenResponse> kcRefresh = keycloakClient.refreshToken(token);
        if (kcRefresh.isPresent()) {
            return AuthResponse.builder()
                    .accessToken(kcRefresh.get().accessToken())
                    .refreshToken(kcRefresh.get().refreshToken())
                    .tokenType("Bearer")
                    .expiresIn(kcRefresh.get().expiresIn())
                    .message("Token refreshed via Keycloak OIDC")
                    .build();
        }

        throw new InvalidCredentialsException(messageService.getMessage(MessageCode.TOKEN_INVALID));
    }

    public void logout(String accessToken, String refreshToken, HttpServletRequest httpRequest) {
        String username = "anonymous";

        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        if (accessToken != null && !accessToken.isBlank()) {
            String jti = extractJti(accessToken);
            if (jti != null) {
                blacklistService.blacklistJti(jti, 86400000);
            }
            username = extractUsername(accessToken);
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            keycloakClient.logout(refreshToken);
            String refreshJti = extractJti(refreshToken);
            if (refreshJti != null) {
                blacklistService.blacklistJti(refreshJti, 604800000);
            }
        }

        recordAudit(username != null ? username : "anonymous", "LOGOUT", "User logged out", httpRequest);
    }

    public TokenValidationResponse validateToken(String token) {
        if (token == null || token.isBlank()) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        JsonNode payload = parseUnverifiedPayload(token);
        if (payload == null) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        String jti = payload.path("jti").asText(null);
        if (jti != null && blacklistService.isJtiBlacklisted(jti)) {
            return TokenValidationResponse.builder().valid(false).message("Token has been revoked").build();
        }

        long exp = payload.path("exp").asLong(0);
        if (exp > 0 && exp < System.currentTimeMillis() / 1000) {
            return TokenValidationResponse.builder().valid(false).message("Token is expired").build();
        }

        String username = payload.has("preferred_username")
                ? payload.path("preferred_username").asText()
                : payload.path("sub").asText(null);

        String role = "ROLE_CUSTOMER";
        List<String> roles = new ArrayList<>();
        JsonNode realmRoles = payload.path("realm_access").path("roles");
        if (realmRoles.isArray() && !realmRoles.isEmpty()) {
            realmRoles.forEach(r -> roles.add(r.asText()));
            role = roles.get(0);
        }

        UUID userId = null;
        if (payload.has("userId")) {
            try {
                userId = UUID.fromString(payload.path("userId").asText());
            } catch (Exception ignored) {}
        }

        List<String> permissions = new ArrayList<>();
        JsonNode permsNode = payload.path("permissions");
        if (permsNode.isArray()) {
            permsNode.forEach(p -> permissions.add(p.asText()));
        } else {
            permissions = getPermissionsForRoleName(role);
        }

        return TokenValidationResponse.builder()
                .valid(true)
                .active(true)
                .username(username)
                .role(role)
                .roles(roles)
                .permissions(permissions)
                .userId(userId)
                .email(payload.path("email").asText(null))
                .message("Token is valid")
                .build();
    }

    private String extractJti(String token) {
        JsonNode payload = parseUnverifiedPayload(token);
        return payload != null ? payload.path("jti").asText(null) : null;
    }

    private String extractUsername(String token) {
        JsonNode payload = parseUnverifiedPayload(token);
        if (payload == null) return null;
        if (payload.has("preferred_username")) return payload.path("preferred_username").asText();
        if (payload.has("sub")) return payload.path("sub").asText();
        return payload.path("email").asText(null);
    }

    private JsonNode parseUnverifiedPayload(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;
        try {
            return objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }


    @Transactional
    public MfaSetupResponse setupMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage(MessageCode.USER_NOT_FOUND) + " Username: " + username));

        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrCodeUri = totpService.generateQrCodeUri(secret, user.getEmail(), "Logistics-Platform");

        List<String> backupCodes = List.of(
                UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        return MfaSetupResponse.builder()
                .secret(secret)
                .manualEntryKey(secret)
                .qrCodeUri(qrCodeUri)
                .backupCodes(backupCodes)
                .build();
    }

    @Transactional
    public boolean verifyAndEnableMfa(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage(MessageCode.USER_NOT_FOUND) + " Username: " + username));

        if (user.getMfaSecret() == null) {
            throw new InvalidCredentialsException("MFA secret is not initialized. Please call /api/v1/auth/mfa/setup first.");
        }

        boolean valid = totpService.verifyCode(user.getMfaSecret(), code);
        if (valid) {
            user.setMfaEnabled(true);
            userRepository.save(user);
            recordAudit(username, "MFA_ENABLED", "Two-factor authentication successfully enabled", null);
            return true;
        }
        return false;
    }

    public List<String> getPermissionsForRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Collections.emptyList();
        }
        String standardRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName.toUpperCase();
        List<String> perms = roleRepository.findByCodeWithPermissions(standardRole)
                .map(role -> role.getPermissions() != null
                        ? role.getPermissions().stream()
                                .map(Permission::getCode)
                                .sorted()
                                .toList()
                        : Collections.<String>emptyList())
                .orElse(Collections.emptyList());

        if (!perms.isEmpty()) {
            return perms;
        }

        String clean = standardRole.startsWith("ROLE_") ? standardRole.substring(5) : standardRole;
        return switch (clean.toUpperCase()) {
            case "ADMIN" -> List.of("orders:create", "orders:read", "orders:update", "orders:cancel", "orders:delete", "fleet:view", "fleet:dispatch", "users:read", "users:write", "analytics:view");
            case "MERCHANT" -> List.of("orders:create", "orders:read", "orders:cancel", "analytics:view");
            case "COURIER" -> List.of("orders:read", "orders:status:update", "fleet:status:update");
            case "DISPATCHER" -> List.of("orders:read", "orders:update", "fleet:dispatch", "fleet:view");
            default -> List.of("orders:create", "orders:read", "orders:cancel");
        };
    }

    public List<String> getPermissionsForRole(User.UserRole role) {
        if (role == null) {
            return Collections.emptyList();
        }
        return getPermissionsForRoleName(role.name());
    }

    private void recordAudit(String username, String eventType, String details, HttpServletRequest request) {
        try {
            String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
            String userAgent = request != null ? request.getHeader("User-Agent") : "Internal-Service";

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
