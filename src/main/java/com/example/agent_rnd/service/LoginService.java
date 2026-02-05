package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.auth.LoginRequest;
import com.example.agent_rnd.dto.auth.LoginResponse;
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

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 3. 토큰 생성 (여기엔 role 정보가 암호화되어 들어감)
        String token = jwtTokenProvider.createToken(user.getUserId(), user.getRole().name());

        // 4. 토큰만 딱 반환!
        return new LoginResponse(token);
    }
}