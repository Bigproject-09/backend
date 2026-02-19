package com.example.agent_rnd.dto;

public class AuthDtos {

    public record CompanySignupRequest(
            String email,
            String password,
            String passwordConfirm,
            String authCode,
            String name,
            String department,
            String position
    ) {}

    public record CompanySignupResult(Long companyId, Long userId) {}

    public record CompanySignupResponse(Long companyId, Long userId) {}
}
