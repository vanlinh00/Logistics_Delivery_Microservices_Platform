package com.logistics.auth.service;

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
import com.logistics.auth.security.JwtProvider;
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
    private final JwtProvider jwtProvider;
    private final KeycloakClient keycloakClient;
    private final TokenBlacklistService blacklistService;
    private final TotpService totpService;
    private final MessageService messageService;

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

        // 5. Try Keycloak Direct Grant token exchange or generate internal JWT
        Optional<KeycloakClient.KeycloakTokenResponse> kcToken = keycloakClient.login(user.getUsername(), request.getPassword());

        String accessToken;
        String refreshToken;
        long expiresIn;

        if (kcToken.isPresent()) {
            accessToken = kcToken.get().accessToken();
            refreshToken = kcToken.get().refreshToken();
            expiresIn = kcToken.get().expiresIn();
            log.info("Acquired OIDC token from Keycloak for user {}", user.getUsername());
        } else {
            accessToken = jwtProvider.generateToken(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name(),
                    user.getEmail(),
                    user.getFullName()
            );
            refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getUsername());
            expiresIn = jwtProvider.getExpirationMs() / 1000;
        }

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
                .permissions(getPermissionsForRole(user.getRole()))
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

        String accessToken = jwtProvider.generateToken(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().name(),
                savedUser.getEmail(),
                savedUser.getFullName()
        );
        String refreshToken = jwtProvider.generateRefreshToken(savedUser.getId(), savedUser.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getExpirationMs() / 1000)
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .fullName(savedUser.getFullName())
                .permissions(getPermissionsForRole(savedUser.getRole()))
                .message(messageService.getMessage(MessageCode.CREATED))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException(messageService.getMessage(MessageCode.TOKEN_INVALID));
        }

        // Try Keycloak token refresh first
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

        // Validate local refresh token
        if (!jwtProvider.validateToken(token)) {
            throw new InvalidCredentialsException(messageService.getMessage(MessageCode.TOKEN_INVALID));
        }

        String username = jwtProvider.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage(MessageCode.USER_NOT_FOUND) + " Username: " + username));

        String newAccess = jwtProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getEmail(),
                user.getFullName()
        );
        String newRefresh = jwtProvider.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getExpirationMs() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Token refreshed successfully")
                .build();
    }

    public void logout(String accessToken, String refreshToken, HttpServletRequest httpRequest) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        if (accessToken != null && !accessToken.isBlank()) {
            String jti = jwtProvider.getJtiFromToken(accessToken);
            if (jti != null && !jti.isBlank()) {
                blacklistService.blacklistJti(jti, jwtProvider.getExpirationMs());
                log.info("Access token jti [{}] blacklisted on logout", jti);
            } else {
                // Fallback for non-standard or opaque tokens
                blacklistService.blacklistJti(accessToken, jwtProvider.getExpirationMs());
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            keycloakClient.logout(refreshToken);
            String refreshJti = jwtProvider.getJtiFromToken(refreshToken);
            if (refreshJti != null && !refreshJti.isBlank()) {
                blacklistService.blacklistJti(refreshJti, jwtProvider.getRefreshExpirationMs());
                log.info("Refresh token jti [{}] blacklisted on logout", refreshJti);
            } else {
                blacklistService.blacklistJti(refreshToken, jwtProvider.getRefreshExpirationMs());
            }
        }

        String username = accessToken != null ? jwtProvider.getUsernameFromToken(accessToken) : "anonymous";
        recordAudit(username, "LOGOUT", "User logged out", httpRequest);
    }

    public TokenValidationResponse validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtProvider.validateToken(token)) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        String username = jwtProvider.getUsernameFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);
        List<String> roles = jwtProvider.getAllRolesFromToken(token);
        UUID userId = jwtProvider.getUserIdFromToken(token);
        String email = jwtProvider.getEmailFromToken(token);

        // If userId or email is missing from token claims, enrich from Postgres User DB
        if ((userId == null || email == null) && username != null) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                if (userId == null) userId = u.getId();
                if (email == null) email = u.getEmail();
                if (role == null || role.equals("ROLE_CUSTOMER")) role = u.getRole().name();
            }
        }

        List<String> permissions = getPermissionsForRoleName(role);

        return TokenValidationResponse.builder()
                .valid(true)
                .username(username)
                .role(role)
                .roles(roles)
                .permissions(permissions)
                .userId(userId)
                .email(email)
                .active(true)
                .build();
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
        return roleRepository.findByCodeWithPermissions(standardRole)
                .map(role -> role.getPermissions() != null
                        ? role.getPermissions().stream()
                                .map(Permission::getCode)
                                .sorted()
                                .toList()
                        : Collections.<String>emptyList())
                .orElse(Collections.emptyList());
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
