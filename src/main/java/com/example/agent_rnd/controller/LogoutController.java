package com.example.agent_rnd.controller;

import com.example.agent_rnd.service.LogoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logout")
public class LogoutController {

    private final LogoutService logoutService;

    @PostMapping
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더(Bearer 토큰)가 필요합니다.");
        }
        String token = authorization.substring(7).trim();
        logoutService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
