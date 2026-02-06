package com.example.agent_rnd.service;

import com.example.agent_rnd.dto.mypage.AuditLogDto;
import com.example.agent_rnd.dto.mypage.ProjectDto;
import com.example.agent_rnd.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final ProposalRepository proposalRepository;

    // 1. [멤버] 내 프로젝트 조회
    public List<ProjectDto> getMyProjects(Long userId) {
        // DB에서 내가 쓴 제안서 찾아서 DTO로 변환
        return proposalRepository.findByUser_UserId(userId).stream()
                .map(p -> new ProjectDto(
                        p.getProposalId(),
                        p.getTitle(),
                        "작성중", // Proposal 엔티티에 상태 필드가 없다면 일단 고정값 or 로직 추가
                        p.getCreatedAt()
                ))
                .toList();
    }

    // 2. [관리자] 감사 로그 조회
    public List<AuditLogDto> getAuditLogs() {
        // TODO: 추후 실제 AuditLogRepository 연결 필요
        // 현재는 UI 테스트를 위한 더미 데이터 반환
        return List.of(
                new AuditLogDto(1L, "김철수(member)", "LOGIN", "시스템 접속", LocalDateTime.now().minusHours(2)),
                new AuditLogDto(2L, "박영희(member)", "GENERATE", "AI_바우처_제안서.pdf", LocalDateTime.now().minusMinutes(45)),
                new AuditLogDto(3L, "이민수(member)", "DOWNLOAD", "2025_사업계획서_v2.docx", LocalDateTime.now().minusMinutes(10))
        );
    }
}