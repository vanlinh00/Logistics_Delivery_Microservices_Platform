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

    public void blacklistToken(String token, long expirationMillis) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            long ttlSeconds = Math.max(expirationMillis / 1000, 60);
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "revoked",
                    Duration.ofSeconds(ttlSeconds)
            );
            log.info("Token blacklisted in Redis for {} seconds", ttlSeconds);
        } catch (Exception e) {
            log.warn("Redis unavailable for token blacklisting, continuing: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis unavailable during blacklist check: {}", e.getMessage());
            return false;
        }
    }
}
