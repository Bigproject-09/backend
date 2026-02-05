package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    // 1. [멤버] 내 프로젝트 (작성 중인 제안서 목록)
    // 일반 멤버, 관리자 모두 본인의 제안서를 볼 수 있어야 하므로 권한 제한 없음 (로그인만 하면 됨)
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDto>> getMyProjects(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(myPageService.getMyProjects(userId));
    }

    // 2. [관리자] 감사 로그 (보안 감사)
    // ★ 관리자(ADMIN)만 접근 가능
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs() {
        return ResponseEntity.ok(myPageService.getAuditLogs());
    }
}