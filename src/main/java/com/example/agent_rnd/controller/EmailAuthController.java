package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.EmailAuthDtos;
import com.example.agent_rnd.repository.UserRepository;
import com.example.agent_rnd.service.EmailAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/email")
public class EmailAuthController {

    private final EmailAuthService emailAuthService;
    private final UserRepository userRepository;

    // 이메일 중복 확인
    @PostMapping("/check")
    public ResponseEntity<EmailAuthDtos.CheckEmailResponse> check(@RequestBody EmailAuthDtos.CheckEmailRequest req) {
        boolean exists = userRepository.existsByEmail(req.email());
        boolean available = !exists;

        String msg = available ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.";
        return ResponseEntity.ok(new EmailAuthDtos.CheckEmailResponse(available, msg));
    }

    @PostMapping("/send")
    public ResponseEntity<EmailAuthDtos.SendCodeResponse> send(@RequestBody EmailAuthDtos.SendCodeRequest req) {
        emailAuthService.sendCode(req.email());
        return ResponseEntity.ok(new EmailAuthDtos.SendCodeResponse("인증코드를 발송했습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<EmailAuthDtos.VerifyCodeResponse> verify(@RequestBody EmailAuthDtos.VerifyCodeRequest req) {
        boolean ok = emailAuthService.verifyCode(req.email(), req.code());
        return ResponseEntity.ok(new EmailAuthDtos.VerifyCodeResponse(ok, "인증 완료"));
    }
}
