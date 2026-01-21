package com.example.agent_rnd.controller.api.auth;

import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.domain.user.dto.LoginRequest;
import com.example.agent_rnd.domain.user.dto.LoginResponse;
import com.example.agent_rnd.service.UserService;
import com.example.agent_rnd.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        // 1️⃣ 로그인 검증
        User user = userService.login(request);

        // 2️⃣ JWT 생성
        String token = JwtProvider.createToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // 3️⃣ 응답
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                token
        );
    }
}

