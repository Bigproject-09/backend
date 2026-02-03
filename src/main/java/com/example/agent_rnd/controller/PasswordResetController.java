package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.PasswordResetDtos;
import com.example.agent_rnd.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/reset")
    public ResponseEntity<PasswordResetDtos.ResetPasswordResponse> reset(
            @RequestBody PasswordResetDtos.ResetPasswordRequest req
    ) {
        passwordResetService.resetPassword(
                req.email(),
                req.currentPassword(),
                req.newPassword(),
                req.newPasswordConfirm()
        );
        return ResponseEntity.ok(new PasswordResetDtos.ResetPasswordResponse("비밀번호가 변경되었습니다."));
    }
}
