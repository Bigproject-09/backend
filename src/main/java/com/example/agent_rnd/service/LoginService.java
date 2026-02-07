package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.auditlog.AuditLog; // ★ 추가
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.auth.LoginRequest;
import com.example.agent_rnd.dto.auth.LoginResponse;
import com.example.agent_rnd.repository.AuditLogRepository; // ★ 추가
import com.example.agent_rnd.repository.UserRepository;
import com.example.agent_rnd.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogRepository auditLogRepository; // ★ 1. 의존성 추가

    // 생성자 주입
    public LoginService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        AuditLogRepository auditLogRepository) { // ★ 생성자에도 추가
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogRepository = auditLogRepository;
    }

    // ★ 2. 읽기 전용(readOnly=true) 제거 (저장을 해야 하니까요!)
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 3. 토큰 생성
        String token = jwtTokenProvider.createToken(user.getUserId(), user.getRole().name());

        // ★ 4. [핵심] 로그인 성공 로그 저장
        auditLogRepository.save(AuditLog.builder()
                .user(user)             // 누가 (방금 로그인한 유저)
                .action("LOGIN")        // 무엇을 (로그인)
                .targetDocument("시스템 접속") // 내용
                .build());

        // 5. 토큰 반환
        return new LoginResponse(token);
    }
}