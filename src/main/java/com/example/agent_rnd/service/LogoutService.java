package com.example.agent_rnd.service;

import com.example.agent_rnd.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final StringRedisTemplate redis;
    private final JwtTokenProvider jwtTokenProvider;

    private String blacklistKey(String token) {
        return "auth:jwt:blacklist:" + token;
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰이 없습니다.");
        }
        token = token.trim();

        if (!jwtTokenProvider.validateToken(token)) {
            // 이미 만료/위조면 굳이 저장할 필요 없음
            return;
        }

        long ttlMs = jwtTokenProvider.getRemainingMillis(token);
        if (ttlMs <= 0) return;

        redis.opsForValue().set(blacklistKey(token), "1", Duration.ofMillis(ttlMs));
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        return "1".equals(redis.opsForValue().get(blacklistKey(token.trim())));
    }
}
