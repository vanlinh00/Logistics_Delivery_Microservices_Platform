package com.logistics.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Blacklist a token by its unique JWT ID (jti)
     * Format in Redis: jwt:blacklist:<jti>
     *
     * @param jti JWT unique ID
     * @param expirationMillis remaining TTL for the token in milliseconds
     */
    public void blacklistJti(String jti, long expirationMillis) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        try {
            long ttlSeconds = Math.max(expirationMillis / 1000, 60);
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "revoked",
                    Duration.ofSeconds(ttlSeconds)
            );
            log.info("JWT jti [{}] blacklisted in Redis for {} seconds (Key: {}{})", jti, ttlSeconds, BLACKLIST_PREFIX, jti);
        } catch (Exception e) {
            log.warn("Redis unavailable for jti blacklisting, continuing: {}", e.getMessage());
        }
    }

    /**
     * Check if a specific jti is revoked/blacklisted
     */
    public boolean isJtiBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis unavailable during jti blacklist check: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Backward-compatible helper that blacklists by jti if identifiable, or falls back to raw token/identifier
     */
    public void blacklistToken(String tokenOrJti, long expirationMillis) {
        blacklistJti(tokenOrJti, expirationMillis);
    }

    public boolean isBlacklisted(String tokenOrJti) {
        return isJtiBlacklisted(tokenOrJti);
    }
}
