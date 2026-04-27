package com.example.backend.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Quan ly danh sach token da bi thu hoi (logout tuong minh).
 * Key: "token:blacklist:{token}" — value "1", TTL = thoi gian con lai cua token.
 * JwtAuthenticationFilter kiem tra key nay truoc khi cho phep request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Them token vao blacklist voi TTL = thoi gian con lai truoc khi het han.
     * Neu token da het han roi thi bo qua (khong can luu).
     */
    public void blacklist(String token, Date expiration) {
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            // Token da het han, khong can blacklist
            return;
        }
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + token, "1", Duration.ofMillis(ttlMillis));
        log.debug("Token blacklisted, expires in {}ms", ttlMillis);
    }

    /** Kiem tra token co trong blacklist khong. */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
