package com.example.agent_rnd.dto;

public class AuthDtos {

    // ★ 회원가입 요청 (최종_진짜_최종)
    public record CompanySignupRequest(
            String email,           // 아이디
            String password,        // 비밀번호
            String passwordConfirm, // 비밀번호 확인
            String authCode         // 이메일 인증번호
    ) {}

    public record CompanySignupResult(Long companyId, Long userId) {}
    public record CompanySignupResponse(Long companyId, Long userId) {}
}