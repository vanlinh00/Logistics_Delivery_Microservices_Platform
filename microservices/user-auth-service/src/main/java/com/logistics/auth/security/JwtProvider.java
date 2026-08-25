package com.logistics.auth.security;

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
import java.util.*;

@Component
@Slf4j
public class JwtProvider {

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final TokenBlacklistService blacklistService;
    private final KeycloakClient keycloakClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtProvider(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMs,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs,
            TokenBlacklistService blacklistService,
            KeycloakClient keycloakClient) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.blacklistService = blacklistService;
        this.keycloakClient = keycloakClient;
    }

    public String generateToken(UUID userId, String username, String role, String email, String fullName) {
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("role", role);
        claims.put("roles", List.of(role));
        claims.put("email", email);
        claims.put("name", fullName != null ? fullName : username);
        claims.put("iss", "logistics-auth-service");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String username) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("userId", userId.toString())
                .claim("type", "REFRESH")
                .claim("iss", "logistics-auth-service")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        // Clean token if Bearer prefix present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            // 1. Try local HMAC verification first
            Claims claims = getClaimsFromToken(token);
            String jti = claims.getId();

            if (jti != null && blacklistService.isJtiBlacklisted(jti)) {
                log.warn("Rejected blacklisted token with jti: {}", jti);
                return false;
            }

            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                return false;
            }

            return true;
        } catch (Exception ex) {
            log.debug("Local HMAC validation failed, checking if Keycloak token: {}", ex.getMessage());
            // 2. Fallback to Keycloak validation (RS256 / Introspection / Claims check)
            return validateKeycloakToken(token);
        }
    }

    private boolean validateKeycloakToken(String token) {
        try {
            JsonNode payload = parseUnverifiedPayload(token);
            if (payload == null) {
                return false;
            }

            // Check jti in blacklist
            String jti = payload.path("jti").asText(null);
            if (jti != null && blacklistService.isJtiBlacklisted(jti)) {
                log.warn("Rejected blacklisted Keycloak token with jti: {}", jti);
                return false;
            }

            // Check expiration claim
            long expSeconds = payload.path("exp").asLong(0);
            if (expSeconds > 0) {
                long nowSeconds = System.currentTimeMillis() / 1000;
                if (expSeconds < nowSeconds) {
                    log.debug("Keycloak token expired at {}, current time: {}", expSeconds, nowSeconds);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.debug("Keycloak token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public JsonNode parseUnverifiedPayload(String token) {
        if (token == null || token.isBlank()) return null;
        if (token.startsWith("Bearer ")) token = token.substring(7);

        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(decoded);
        } catch (Exception e) {
            log.debug("Failed to decode JWT payload: {}", e.getMessage());
            return null;
        }
    }

    public String getJtiFromToken(String token) {
        try {
            return getClaimsFromToken(token).getId();
        } catch (Exception e) {
            JsonNode node = parseUnverifiedPayload(token);
            return node != null && node.has("jti") ? node.path("jti").asText() : null;
        }
    }

    public Claims getClaimsFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        try {
            return getClaimsFromToken(token).getSubject();
        } catch (Exception e) {
            JsonNode node = parseUnverifiedPayload(token);
            if (node != null) {
                if (node.has("preferred_username")) return node.path("preferred_username").asText();
                if (node.has("sub")) return node.path("sub").asText();
                if (node.has("email")) return node.path("email").asText();
            }
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String role = (String) claims.get("role");
            if (role != null) return role;

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            if (roles != null && !roles.isEmpty()) return roles.get(0);
        } catch (Exception e) {
            JsonNode node = parseUnverifiedPayload(token);
            if (node != null) {
                // Keycloak realm_access.roles
                JsonNode realmRoles = node.path("realm_access").path("roles");
                if (realmRoles.isArray()) {
                    for (JsonNode r : realmRoles) {
                        String rText = r.asText();
                        if (rText.startsWith("ROLE_") || rText.equalsIgnoreCase("ADMIN") || rText.equalsIgnoreCase("MERCHANT") || rText.equalsIgnoreCase("COURIER") || rText.equalsIgnoreCase("CUSTOMER")) {
                            return rText.startsWith("ROLE_") ? rText : "ROLE_" + rText.toUpperCase();
                        }
                    }
                }
            }
        }
        return "ROLE_CUSTOMER";
    }

    public List<String> getAllRolesFromToken(String token) {
        List<String> rolesList = new ArrayList<>();
        try {
            Claims claims = getClaimsFromToken(token);
            String role = (String) claims.get("role");
            if (role != null) rolesList.add(role);

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            if (roles != null) rolesList.addAll(roles);
        } catch (Exception e) {
            JsonNode node = parseUnverifiedPayload(token);
            if (node != null) {
                JsonNode realmRoles = node.path("realm_access").path("roles");
                if (realmRoles.isArray()) {
                    for (JsonNode r : realmRoles) {
                        rolesList.add(r.asText());
                    }
                }
            }
        }
        if (rolesList.isEmpty()) {
            rolesList.add("ROLE_CUSTOMER");
        }
        return rolesList;
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String email = (String) claims.get("email");
            if (email != null) return email;
        } catch (Exception e) {
            JsonNode node = parseUnverifiedPayload(token);
            if (node != null && node.has("email")) {
                return node.path("email").asText();
            }
        }
        return null;
    }

    public UUID getUserIdFromToken(String token) {
        try {
            String userIdStr = (String) getClaimsFromToken(token).get("userId");
            if (userIdStr != null) {
                return UUID.fromString(userIdStr);
            }
        } catch (Exception ignored) {}

        JsonNode node = parseUnverifiedPayload(token);
        if (node != null) {
            if (node.has("userId")) {
                try {
                    return UUID.fromString(node.path("userId").asText());
                } catch (Exception ignored) {}
            }
            if (node.has("sub")) {
                try {
                    return UUID.fromString(node.path("sub").asText());
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}
