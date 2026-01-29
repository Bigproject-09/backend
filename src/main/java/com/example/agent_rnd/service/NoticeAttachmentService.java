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
    private final NoticeAttachmentParseService noticeAttachmentParseService;

    /**
     * 사용자 첨부파일 업로드
     * - DB 저장은 즉시 확정
     * - 파싱은 분리 수행
     */
    @Transactional
    public NoticeAttachment upload(
            Long noticeId,
            Long userId,
            MultipartFile file
    ) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalStateException("공고 없음"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("유저 없음"));

        NoticeAttachment attachment = NoticeAttachment.create(
                notice,
                user,
                file.getOriginalFilename()
        );

        NoticeAttachment saved = noticeAttachmentRepository.save(attachment);

        startParsing(saved.getAttachmentId(), file);

        return saved;
    }

    /**
     * 파싱 시작
     */
    public void startParsing(Long attachmentId, MultipartFile file) {
        try {
            markProcessing(attachmentId);

            String parsedJson = noticeAttachmentParseService.parse(file);

            completeParsing(attachmentId, parsedJson);

        } catch (Exception e) {
            failParsing(attachmentId, e.getMessage());
        }
    }

    /* =========================
       상태 관리 메서드
       ========================= */

    @Transactional
    public void markProcessing(Long attachmentId) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalStateException("첨부파일 없음"));
        attachment.markProcessing();
    }

    @Transactional
    public void completeParsing(Long attachmentId, String parsedJson) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalStateException("첨부파일 없음"));
        attachment.markDone(parsedJson);
    }

    @Transactional
    public void failParsing(Long attachmentId, String errorMsg) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalStateException("첨부파일 없음"));
        attachment.markFailed(errorMsg);
    }
}

