package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.enums.PresentationStatus;
import com.example.agent_rnd.domain.presentation.Presentation;
import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.script.Script;
import com.example.agent_rnd.dto.ScriptSaveRequest;
import com.example.agent_rnd.repository.PresentationRepository;
import com.example.agent_rnd.repository.ProposalRepository;
import com.example.agent_rnd.repository.ScriptRepository;
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

    @Transactional
    public void saveScript(ScriptSaveRequest request, Long userId) {
        // 1. Proposal 조회 (notice_id와 user_id로)
        Proposal proposal = proposalRepository.findByNotice_NoticeIdAndUser_UserId(
                        request.getNoticeId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("제안서를 찾을 수 없습니다."));

        // 2. Presentation 생성
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

        // 4. QNA는 나중에 별도 처리 (현재는 scripts 테이블에만 저장)
    }
}