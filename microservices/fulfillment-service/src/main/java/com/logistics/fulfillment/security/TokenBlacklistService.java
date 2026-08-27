package com.logistics.fulfillment.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 🛡️ TokenBlacklistService:
 * Validates JWT revocation status against centralized Redis token blacklist cache.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklistJti(String jti, long expirationMillis) {
        if (jti == null || jti.isBlank()) {
            return;
        }

        try {
            long ttlSeconds = Math.max(expirationMillis / 1000, 60);
            String key = BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "revoked", Duration.ofSeconds(ttlSeconds));
            log.info("JWT jti [{}] blacklisted in Redis for {} seconds", jti, ttlSeconds);
        } catch (Exception ex) {
            log.warn("Redis unavailable for jti blacklisting, continuing: {}", ex.getMessage());
        }
    }

    public boolean isJtiBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception ex) {
            log.warn("Redis unavailable during jti blacklist check: {}", ex.getMessage());
            return false;
        }
    }

    public void blacklistToken(String tokenOrJti, long expirationMillis) {
        blacklistJti(tokenOrJti, expirationMillis);
    }

    public boolean isBlacklisted(String tokenOrJti) {
        return isJtiBlacklisted(tokenOrJti);
    }
}
