package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.AuthDtos;
import com.example.agent_rnd.dto.InviteDtos;
import com.example.agent_rnd.service.InviteService;
import com.example.agent_rnd.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final InviteService inviteService;

    // 회사(마스터) 회원가입
    @PostMapping("/company-signup")
    public AuthDtos.CompanySignupResponse companySignup(@RequestBody AuthDtos.CompanySignupRequest req) {
        var r = userService.companySignupAndCreateAdmin(req);
        return new AuthDtos.CompanySignupResponse(r.companyId(), r.adminUserId());
    }

    // 회사 삭제(테스트/관리용)
    @DeleteMapping("/company/{companyId}")
    public ResponseEntity<Void> deleteCompanySignup(@PathVariable Long companyId) {
        userService.deleteCompanySignup(companyId);
        return ResponseEntity.noContent().build();
    }

    // 초대 링크로 들어온 사람이 가입 (permitAll)
    @PostMapping("/invite-signup")
    public ResponseEntity<InviteDtos.InviteSignupResponse> inviteSignup(@RequestBody InviteDtos.InviteSignupRequest req) {
        return ResponseEntity.ok(inviteService.signupByInvite(req));
    }
}
