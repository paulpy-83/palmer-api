package com.palmar.palmer.api.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "jti:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jti, Duration ttl) {
        log.debug("[BLACKLIST] SET {} con TTL {}s", PREFIX + jti, ttl.toSeconds());
        redisTemplate.opsForValue().set(PREFIX + jti, "", ttl);
        log.debug("[BLACKLIST] SET completado OK");
    }

    public boolean isBlacklisted(String jti) {
        boolean result = Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + jti));
        log.debug("[BLACKLIST] CHECK {} → {}", PREFIX + jti, result ? "BLACKLISTED" : "ok");
        return result;
    }
}
