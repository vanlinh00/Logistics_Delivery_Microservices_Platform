package com.logistics.auth.controller;

import com.logistics.auth.constant.ApiPath;
import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.dto.AuthDTOs.*;
import com.logistics.auth.service.AuthService;
import com.logistics.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping(ApiPath.AUTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Authentication & OIDC IAM Bridge", description = "Enterprise Login, Registration, Keycloak Token Exchange, MFA/TOTP, and Session Revocation")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping(ApiPath.LOGIN)
    @Operation(summary = "Authenticate user (Keycloak OIDC / Local) and return JWT token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response, response.getMessage()));
    }

    @PostMapping(ApiPath.REGISTER)
    @Operation(summary = "Register customer, merchant, or courier account with auto-profile setup")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "User registered successfully"));
    }

    @PostMapping(ApiPath.REFRESH)
    @Operation(summary = "Rotate & refresh JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed"));
    }

    @PostMapping(ApiPath.LOGOUT)
    @Operation(summary = "Logout user and invalidate token in Redis Blacklist & Keycloak")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(authHeader, refreshToken, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @GetMapping(ApiPath.VALIDATE)
    @Operation(summary = "Validate JWT Token from Authorization header or query param and return principal role/userId")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        String tokenToValidate = null;
        if (authHeader != null && !authHeader.isBlank()) {
            tokenToValidate = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        } else if (tokenParam != null && !tokenParam.isBlank()) {
            tokenToValidate = tokenParam.startsWith("Bearer ") ? tokenParam.substring(7) : tokenParam;
        }

        TokenValidationResponse validation = authService.validateToken(tokenToValidate);
        return ResponseEntity.ok(ApiResponse.ok(validation, "Token validation result"));
    }

    @GetMapping(ApiPath.ME)
    @Operation(summary = "Get current authenticated user context, profiles, and permissions")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("401", "Unauthorized", null));
        }
        UserSummaryDTO summary = userService.getUserSummary(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(summary, "Current user context"));
    }

    @PostMapping(ApiPath.MFA_SETUP)
    @Operation(summary = "Generate TOTP secret and QR code URI for 2FA setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MfaSetupResponse response = authService.setupMfa(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "MFA setup initialized"));
    }

    @PostMapping(ApiPath.MFA_VERIFY)
    @Operation(summary = "Verify TOTP code and enable 2FA on account")
    public ResponseEntity<ApiResponse<Boolean>> verifyMfa(
            Principal principal,
            @Valid @RequestBody MfaVerifyRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean enabled = authService.verifyAndEnableMfa(principal.getName(), request.getCode());
        return ResponseEntity.ok(ApiResponse.ok(enabled, enabled ? "2FA enabled successfully" : "Invalid 2FA verification code"));
    }
}
