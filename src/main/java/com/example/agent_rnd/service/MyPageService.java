package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.auditlog.AuditLog;
import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.repository.AuditLogRepository;
import com.example.agent_rnd.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // ★ 추가
import org.springframework.data.domain.Pageable; // ★ 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final ProposalRepository proposalRepository;
    private final AuditLogRepository auditLogRepository;

    // 1. [멤버] 내 프로젝트 조회 (기존 유지)
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

    // 2. [관리자] 감사 로그 조회 (페이징 적용)
    // ★ List -> Page로 변경, 파라미터로 Pageable을 받습니다.
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        // DB에서 페이지 설정(pageable)대로 데이터를 가져옴 (예: 0페이지 10개)
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);

        // Entity -> DTO 변환 (Page의 .map 기능을 사용)
        return logs.map(log -> new AuditLogDto(
                log.getId(),
                log.getUser().getEmail() + " (" + log.getUser().getRole() + ")",
                log.getAction(),
                log.getTargetDocument(),
                log.getTimestamp()
        ));
    }
}