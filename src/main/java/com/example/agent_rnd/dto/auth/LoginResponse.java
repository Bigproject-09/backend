package com.example.agent_rnd.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String accessToken; // 기존 호환성을 위해 이름 유지 (또는 token으로 변경 가능)
    private String email;
    private String name;

    // ★ 프론트엔드 분기용 Role 정보 (ADMIN / MEMBER)
    private String role;
}