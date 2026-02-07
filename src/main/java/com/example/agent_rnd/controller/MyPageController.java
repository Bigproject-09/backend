package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // ★ 추가
import org.springframework.data.domain.Pageable; // ★ 추가
import org.springframework.data.domain.Sort; // ★ 추가
import org.springframework.data.web.PageableDefault; // ★ 추가
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

    // 1. [멤버] 내 프로젝트 (기존 유지)
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDto>> getMyProjects(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(myPageService.getMyProjects(userId));
    }

    // 2. [관리자] 감사 로그 (페이징 적용)
    // ★ List -> Page로 변경
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            // ★ 프론트에서 페이지 번호를 안 보내면 기본적으로 0페이지, 10개씩, 최신순으로 가져옴
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 서비스에 pageable(페이지 정보)을 넘겨줌
        return ResponseEntity.ok(myPageService.getAuditLogs(pageable));
    }
}