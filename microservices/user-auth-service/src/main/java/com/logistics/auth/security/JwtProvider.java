package com.logistics.auth.security;

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

    public JwtProvider(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMs,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs,
            TokenBlacklistService blacklistService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.blacklistService = blacklistService;
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

        try {
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
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public String getJtiFromToken(String token) {
        try {
            return getClaimsFromToken(token).getId();
        } catch (Exception e) {
            return null;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String role = (String) claims.get("role");
        if (role != null) return role;

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        if (roles != null && !roles.isEmpty()) return roles.get(0);

        return "ROLE_CUSTOMER";
    }

    public UUID getUserIdFromToken(String token) {
        String userIdStr = (String) getClaimsFromToken(token).get("userId");
        if (userIdStr != null) {
            try {
                return UUID.fromString(userIdStr);
            } catch (Exception ignored) {}
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
