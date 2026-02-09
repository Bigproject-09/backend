package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.auditlog.AuditLog;
import com.example.agent_rnd.domain.project.ProjectMember; // ★ 추가
import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.repository.AuditLogRepository;
import com.example.agent_rnd.repository.ProjectMemberRepository; // ★ 추가
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class    MyPageService {

    // private final ProposalRepository proposalRepository; // 이제 이거 대신 아래 걸 씁니다.
    private final ProjectMemberRepository projectMemberRepository; // ★ 변경
    private final AuditLogRepository auditLogRepository;

    // 1. [멤버] 내 프로젝트 조회 (업그레이드: 초대받은 것도 포함)
    public List<ProjectDto> getMyProjects(Long userId) {
        // 내 멤버 기록(ProjectMember)을 다 찾아서 -> 프로젝트(Proposal) 정보만 추출
        return projectMemberRepository.findByUser_UserId(userId).stream()
                .map(ProjectMember::getProposal)
                .map(p -> new ProjectDto(
                        p.getProposalId(),
                        p.getTitle(),
                        "작성중", // (나중에 p.getStatus()로 교체 가능)
                        p.getCreatedAt()
                ))
                .toList();
    }

    // 2. [관리자] 감사 로그 조회 (기존 유지)
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);
        return logs.map(log -> new AuditLogDto(
                log.getId(),
                log.getUser().getEmail() + " (" + log.getUser().getRole() + ")",
                log.getAction(),
                log.getTargetDocument(),
                log.getTimestamp()
        ));
    }
}