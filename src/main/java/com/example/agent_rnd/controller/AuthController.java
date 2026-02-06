package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.AuthDtos;
import com.example.agent_rnd.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    // private final InviteService inviteService; // ❌ 삭제됨

    // 회원가입 (이제 이 API를 통해 일반 멤버 권한으로 가입됨)
    @PostMapping("/company-signup")
    public AuthDtos.CompanySignupResponse companySignup(@RequestBody AuthDtos.CompanySignupRequest req) {
        // UserService에서 내부적으로 MEMBER 권한으로 생성함
        var r = userService.companySignupAndCreateAdmin(req);

        // 결과 반환 (r.userId()는 이제 일반 멤버의 ID)
        return new AuthDtos.CompanySignupResponse(r.companyId(), r.userId());
    }

    // 회사 및 유저 삭제 (테스트/관리용)
    @DeleteMapping("/company/{companyId}")
    public ResponseEntity<Void> deleteCompanySignup(@PathVariable Long companyId) {
        userService.deleteCompanySignup(companyId);
        return ResponseEntity.noContent().build();
    }

    // ❌ [삭제됨] 초대 링크 가입 기능 (/invite-signup) 제거 완료
}