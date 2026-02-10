package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // 1) [멤버] 내 프로젝트
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDto>> getMyProjects(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(myPageService.getMyProjects(userId));
    }

    // 2) [관리자] 전체 감사 로그 (필터/검색 + 페이징)
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ResponseEntity.ok(myPageService.getAuditLogs(pageable, userId, action, keyword));
    }

    // 3) [멤버/관리자] 내 감사 로그 (페이징)
    @GetMapping("/my-audit-logs")
    public ResponseEntity<Page<AuditLogDto>> getMyAuditLogs(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(myPageService.getMyAuditLogs(userId, pageable));
    }
}
