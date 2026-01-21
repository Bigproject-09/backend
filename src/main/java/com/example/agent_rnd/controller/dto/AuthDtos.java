package com.example.agent_rnd.controller.dto;

public class AuthDtos {

    public record CompanySignupRequest(
            String companyName,
            String businessRegNo,
            String openDate,
            String ceoName,
            String adminEmail,
            String password,
            String passwordConfirm,
            Integer planId
    ) {}

    public record CompanySignupResponse(
            Long companyId,
            Long adminUserId
            //String tempPassword
    ) {}
}
