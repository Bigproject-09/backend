package com.example.agent_rnd.controller;

import com.example.agent_rnd.service.AuditLogService;
import com.example.agent_rnd.service.LogoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logout")
public class LogoutController {

    private final LogoutService logoutService;
    private final AuditLogService auditLogService; // ✅ 추가

    @PostMapping
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @AuthenticationPrincipal Long userId // ✅ 추가
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더(Bearer 토큰)가 필요합니다.");
        }
        String token = authorization.substring(7).trim();

        logoutService.logout(token);

        // ✅ 로그아웃 로그
        auditLogService.log(userId, "LOGOUT", null);

        return ResponseEntity.noContent().build();
    }
}
