package com.example.agent_rnd.dto;

import java.util.List;
import java.util.Map;

public class AuthDtos {

    // 회원가입 요청 (Request)
    public record CompanySignupRequest(
            String companyName,
            String businessRegNo,
            String openDate,   // YYYYMMDD
            String ceoName,
            String email,
            String password,
            String passwordConfirm,
            Integer planId,

            // RegistrationPage에서 추가로 보내는 값들(옵션)
            String address,
            String industry,
            Long employees,
            Map<String, Object> financialSummary,
            List<String> history,
            List<String> coreCompetency
    ) {}

    // ★ [추가] 서비스 내부 반환용 (UserService에서 사용 중인 그 친구!)
    public record CompanySignupResult(Long companyId, Long userId) {}

    // 응답용 (Controller -> Frontend)
    public record CompanySignupResponse(Long companyId, Long adminUserId) {}
}