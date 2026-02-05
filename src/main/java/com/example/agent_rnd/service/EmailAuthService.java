package com.example.agent_rnd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;

    @Value("${auth.email.code-ttl-seconds:300}")
    private long codeTtlSeconds;

    @Value("${auth.email.verified-ttl-seconds:3600}")
    private long verifiedTtlSeconds;

    @Value("${auth.email.max-attempts:5}")
    private int maxAttempts;

    private static final SecureRandom random = new SecureRandom();

    private String codeKey(String email) { return "auth:email:code:" + email; }
    private String attemptKey(String email) { return "auth:email:attempt:" + email; }
    private String verifiedKey(String email) { return "auth:email:verified:" + email; }

    @Transactional
    public void sendCode(String email) {
        email = normalize(email);

        String code = String.format("%06d", random.nextInt(1_000_000));

        // 코드 저장 (TTL 5분)
        redis.opsForValue().set(codeKey(email), code, Duration.ofSeconds(codeTtlSeconds));
        // 시도횟수 초기화 (코드 TTL과 동일하게)
        redis.opsForValue().set(attemptKey(email), "0", Duration.ofSeconds(codeTtlSeconds));
        // 이전 인증완료 플래그 제거
        redis.delete(verifiedKey(email));

        // 메일 발송
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("[RanDi] 이메일 인증 코드");
        msg.setText("인증코드: " + code + "\n유효시간: " + (codeTtlSeconds / 60) + "분");
        mailSender.send(msg);
    }

    @Transactional
    public boolean verifyCode(String email, String code) {
        email = normalize(email);
        code = (code == null) ? "" : code.trim();

        String saved = redis.opsForValue().get(codeKey(email));
        if (saved == null) {
            throw new IllegalArgumentException("인증코드가 없거나 만료되었습니다.");
        }

        int attempts = parseInt(redis.opsForValue().get(attemptKey(email)), 0);
        if (attempts >= maxAttempts) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 다시 발급받아 주세요.");
        }

        if (!saved.equals(code)) {
            attempts++;
            redis.opsForValue().set(attemptKey(email), String.valueOf(attempts),
                    Duration.ofSeconds(codeTtlSeconds)); // TTL은 대충 유지
            throw new IllegalArgumentException("인증코드가 일치하지 않습니다.");
        }

        // 성공: verified 플래그 저장(1시간)
        redis.opsForValue().set(verifiedKey(email), "1", Duration.ofSeconds(verifiedTtlSeconds));

        // 코드/시도횟수 삭제(선택)
//        redis.delete(codeKey(email));
//        redis.delete(attemptKey(email));

        return true;
    }

    public boolean isVerified(String email) {
        email = normalize(email);
        return "1".equals(redis.opsForValue().get(verifiedKey(email)));
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");
        return email.trim().toLowerCase();
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public void clear(String email) {
        email = normalize(email);
        redis.delete(codeKey(email));
        redis.delete(attemptKey(email));
        redis.delete(verifiedKey(email));
    }

}
