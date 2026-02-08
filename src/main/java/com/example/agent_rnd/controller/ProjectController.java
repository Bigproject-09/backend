package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.project.MemberInviteRequest;
import com.example.agent_rnd.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // 멤버 초대 API
    @PostMapping("/{proposalId}/members")
    public ResponseEntity<String> inviteMember(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long proposalId,
            @RequestBody MemberInviteRequest request
    ) {
        projectService.inviteMember(userId, proposalId, request);
        return ResponseEntity.ok("초대가 완료되었습니다.");
    }
}