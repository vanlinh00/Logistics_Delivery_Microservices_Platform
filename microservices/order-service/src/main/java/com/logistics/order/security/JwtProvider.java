package com.logistics.order.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class JwtProvider {

    private final SecretKey key;
    private final TokenBlacklistService blacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtProvider(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            TokenBlacklistService blacklistService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.blacklistService = blacklistService;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        token = stripBearerPrefix(token);
        try {
            Claims claims = getClaimsFromToken(token);
            if (blacklistService.isJtiBlacklisted(claims.getId())) {
                log.warn("Rejected blacklisted token with jti: {}", claims.getId());
                return false;
            }
            Date expiration = claims.getExpiration();
            return expiration == null || expiration.after(new Date());
        } catch (Exception ex) {
            log.debug("Local HMAC validation failed, checking Keycloak claims: {}", ex.getMessage());
            return validateKeycloakToken(token);
        }
    }

    private boolean validateKeycloakToken(String token) {
        JsonNode payload = parseUnverifiedPayload(token);
        if (payload == null) {
            return false;
        }

        if (blacklistService.isJtiBlacklisted(payload.path("jti").asText(null))) {
            return false;
        }
        long expirationSeconds = payload.path("exp").asLong(0);
        return expirationSeconds == 0 || expirationSeconds > System.currentTimeMillis() / 1000;
    }

    public JsonNode parseUnverifiedPayload(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String[] parts = stripBearerPrefix(token).split("\\.");
        if (parts.length < 2) {
            return null;
        }

        try {
            return objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        } catch (Exception ex) {
            log.debug("Failed to decode JWT payload: {}", ex.getMessage());
            return null;
        }
    }

    public String getJtiFromToken(String token) {
        try {
            return getClaimsFromToken(token).getId();
        } catch (Exception ex) {
            JsonNode payload = parseUnverifiedPayload(token);
            return payload != null ? payload.path("jti").asText(null) : null;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(stripBearerPrefix(token))
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        try {
            return getClaimsFromToken(token).getSubject();
        } catch (Exception ex) {
            JsonNode payload = parseUnverifiedPayload(token);
            if (payload == null) {
                return null;
            }
            if (payload.has("preferred_username")) return payload.path("preferred_username").asText();
            if (payload.has("sub")) return payload.path("sub").asText();
            return payload.path("email").asText(null);
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String role = claims.get("role", String.class);
            if (role != null) return role;
            List<String> roles = claims.get("roles", List.class);
            if (roles != null && !roles.isEmpty()) return roles.get(0);
        } catch (Exception ex) {
            JsonNode roles = parseUnverifiedPayload(token);
            JsonNode realmRoles = roles == null ? null : roles.path("realm_access").path("roles");
            if (realmRoles != null && realmRoles.isArray() && !realmRoles.isEmpty()) {
                String role = realmRoles.get(0).asText();
                return role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
            }
        }
        return "ROLE_CUSTOMER";
    }

    public List<String> getAllRolesFromToken(String token) {
        List<String> roles = new ArrayList<>();
        try {
            Claims claims = getClaimsFromToken(token);
            String role = claims.get("role", String.class);
            if (role != null) roles.add(role);
            List<String> claimRoles = claims.get("roles", List.class);
            if (claimRoles != null) roles.addAll(claimRoles);
        } catch (Exception ex) {
            JsonNode payload = parseUnverifiedPayload(token);
            JsonNode realmRoles = payload == null ? null : payload.path("realm_access").path("roles");
            if (realmRoles != null && realmRoles.isArray()) {
                realmRoles.forEach(role -> roles.add(role.asText()));
            }
        }
        if (roles.isEmpty()) roles.add("ROLE_CUSTOMER");
        return roles;
    }

    public String getEmailFromToken(String token) {
        try {
            return getClaimsFromToken(token).get("email", String.class);
        } catch (Exception ex) {
            JsonNode payload = parseUnverifiedPayload(token);
            return payload != null ? payload.path("email").asText(null) : null;
        }
    }

    public List<String> getPermissionsFromToken(String token) {
        try {
            List<String> permissions = getClaimsFromToken(token).get("permissions", List.class);
            return permissions != null ? permissions : List.of();
        } catch (Exception ex) {
            JsonNode payload = parseUnverifiedPayload(token);
            if (payload == null || !payload.path("permissions").isArray()) {
                return List.of();
            }
            List<String> permissions = new ArrayList<>();
            payload.path("permissions").forEach(permission -> permissions.add(permission.asText()));
            return permissions;
        }
    }

    public UUID getUserIdFromToken(String token) {
        String userId = null;
        try {
            userId = getClaimsFromToken(token).get("userId", String.class);
        } catch (Exception ex) {
            JsonNode payload = parseUnverifiedPayload(token);
            if (payload != null) {
                userId = payload.path("userId").asText(null);
                if (userId == null) userId = payload.path("sub").asText(null);
            }
        }
        try {
            return userId == null ? null : UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String stripBearerPrefix(String token) {
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
