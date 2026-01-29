package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.NoticeAttachmentRepository;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import com.example.agent_rnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NoticeAttachmentService {

    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final ProjectNoticeRepository projectNoticeRepository;
    private final UserRepository userRepository;

    // 🔥 파싱 서비스 주입
    private final NoticeAttachmentParseService noticeAttachmentParseService;

    /**
     * 사용자 첨부파일 업로드 + 즉시 파싱
     */
    @Transactional
    public NoticeAttachment upload(
            Long noticeId,
            Long userId,
            MultipartFile file
    ) {
        // 1️⃣ 공고 조회
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공고 없음"));

        // 2️⃣ 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 3️⃣ 첨부파일 엔티티 생성 (초기 상태: WAIT)
        NoticeAttachment attachment = NoticeAttachment.create(
                notice,
                user,
                file.getOriginalFilename()
        );

        // 4️⃣ 먼저 저장 (PK 확보)
        NoticeAttachment savedAttachment =
                noticeAttachmentRepository.save(attachment);

        // 🔥 5️⃣ 업로드 직후 즉시 파싱 실행
        noticeAttachmentParseService.parseAndSave(
                savedAttachment.getId(),
                file
        );

        // 6️⃣ 결과 반환
        return savedAttachment;
    }

    /**
     * (선택) 파싱 상태를 수동으로 PROCESSING 처리
     * → 현재 구조에서는 parseService가 처리하므로 거의 안 씀
     */
    @Transactional
    public void markProcessing(Long attachmentId) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일 없음"));

        attachment.markProcessing();
    }

    /**
     * (선택) 파싱 성공 처리
     */
    @Transactional
    public void completeParsing(Long attachmentId, String parsedJson) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일 없음"));

        attachment.markDone(parsedJson);
    }

    /**
     * (선택) 파싱 실패 처리
     */
    @Transactional
    public void failParsing(Long attachmentId, String errorMsg) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일 없음"));

        attachment.markFailed(errorMsg);
    }
}
