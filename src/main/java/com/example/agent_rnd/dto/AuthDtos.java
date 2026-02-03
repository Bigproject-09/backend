package com.example.agent_rnd.dto;

import java.util.List;
import java.util.Map;

public class AuthDtos {
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
            String address,                 // 합쳐서 문자열 저장
            String industry,
            Long employees,
            Map<String, Object> financialSummary, // {"assetAmount": 123, ...}
            List<String> history,                  // ["연혁1", "연혁2"...]
            List<String> coreCompetency            // ["기술1", "기술2"...]
    ) {}

    public record CompanySignupResponse(Long companyId, Long adminUserId) {}
}
