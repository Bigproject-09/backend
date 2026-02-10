package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.auditlog.AuditLog;
import com.example.agent_rnd.domain.project.ProjectMember;
import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.repository.AuditLogRepository;
import com.example.agent_rnd.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final ProjectMemberRepository projectMemberRepository;
    private final AuditLogRepository auditLogRepository;

    // 1) [멤버] 내 프로젝트
    public List<ProjectDto> getMyProjects(Long userId) {
        return projectMemberRepository.findByUser_UserId(userId).stream()
                .map(ProjectMember::getProposal)
                .map(p -> new ProjectDto(
                        p.getProposalId(),
                        p.getTitle(),
                        "작성중",
                        p.getCreatedAt()
                ))
                .toList();
    }

    // 2) [관리자] 전체 감사 로그 (필터/검색 + 페이징)
    public Page<AuditLogDto> getAuditLogs(Pageable pageable, Long userId, String action, String keyword) {
        Page<AuditLog> logs = auditLogRepository.searchLogs(userId, action, keyword, pageable);
        return logs.map(this::toDto);
    }

    // 3) [멤버/관리자] 내 감사 로그 (페이징)
    public Page<AuditLogDto> getMyAuditLogs(Long userId, Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findByUser_UserId(userId, pageable);
        return logs.map(this::toDto);
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUser().getEmail() + " (" + log.getUser().getRole() + ")",
                log.getAction(),
                log.getTargetDocument(),
                log.getTimestamp()
        );
    }
}