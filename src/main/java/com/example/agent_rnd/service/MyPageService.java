package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.auditlog.AuditLog; // 패키지 경로 주의
import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.repository.AuditLogRepository;
import com.example.agent_rnd.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final ProposalRepository proposalRepository;
    private final AuditLogRepository auditLogRepository;

    // 1. [멤버] 내 프로젝트 조회
    public List<ProjectDto> getMyProjects(Long userId) {
        return proposalRepository.findByUser_UserId(userId).stream()
                .map(p -> new ProjectDto(
                        p.getProposalId(),
                        p.getTitle(),
                        "작성중",
                        p.getCreatedAt()
                ))
                .toList();
    }

    // 2. [관리자] 감사 로그 조회
    public List<AuditLogDto> getAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));

        return logs.stream()
                .map(log -> new AuditLogDto(
                        log.getId(),
                        // ★ 수정됨: getName() -> getEmail()
                        log.getUser().getEmail() + " (" + log.getUser().getRole() + ")",
                        log.getAction(),
                        log.getTargetDocument(),
                        log.getTimestamp()
                ))
                .collect(Collectors.toList());
    }
}