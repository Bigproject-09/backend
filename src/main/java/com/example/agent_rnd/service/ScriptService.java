package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.auditlog.AuditLog; // ★ 추가
import com.example.agent_rnd.domain.enums.PresentationStatus;
import com.example.agent_rnd.domain.presentation.Presentation;
import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.script.Script;
import com.example.agent_rnd.domain.user.User; // ★ 추가
import com.example.agent_rnd.dto.ScriptSaveRequest;
import com.example.agent_rnd.repository.*; // Repository들
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScriptService {

    private final ScriptRepository scriptRepository;
    private final PresentationRepository presentationRepository;
    private final ProposalRepository proposalRepository;

    // ★ 로그 저장을 위해 추가
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveScript(ScriptSaveRequest request, Long userId) {
        // 1. Proposal 조회
        Proposal proposal = proposalRepository.findByNotice_NoticeIdAndUser_UserId(
                        request.getNoticeId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("제안서를 찾을 수 없습니다."));

        // 2. Presentation 생성 (PPT 확정)
        Presentation presentation = Presentation.builder()
                .proposal(proposal)
                .status(PresentationStatus.COMPLETED)
                .totalTokens(0)
                .version(1)
                .createdAt(LocalDateTime.now())
                .build();
        presentationRepository.save(presentation);

        // 3. Scripts 저장
        for (ScriptSaveRequest.SlideDto slide : request.getSlides()) {
            Script script = Script.builder()
                    .presentation(presentation)
                    .pageNo(slide.getPage())
                    .textContent(slide.getScript())
                    .build();
            scriptRepository.save(script);
        }

        // ★ 4. [보안 로그 추가] 생성(GENERATE) 로그 남기기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보 없음"));

        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action("GENERATE") // 활동: 생성
                .targetDocument("PPT 확정 및 대본 V1") // (여기 파일명 대신 제목을 넣어도 됩니다)
                .build());
    }
}