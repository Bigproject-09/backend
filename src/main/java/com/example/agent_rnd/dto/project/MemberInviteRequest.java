package com.example.agent_rnd.dto.project;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberInviteRequest {
    private String email; // 초대할 이메일
    private String role;  // "MEMBER" or "ADMIN"
}