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

        User user = userService.login(request);

        // 1. getId() -> getUserId() 수정
        String token = JwtProvider.createToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );

        // 2. getId() -> getUserId() 수정
        return new LoginResponse(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                token
        );
    }
}