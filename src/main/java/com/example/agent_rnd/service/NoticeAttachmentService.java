package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import com.example.agent_rnd.domain.notice.NoticeFile;
import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.NoticeAttachmentRepository;
import com.example.agent_rnd.repository.NoticeFileRepository;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import com.example.agent_rnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoticeAttachmentService {

    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final NoticeFileRepository noticeFileRepository;
    private final ProjectNoticeRepository projectNoticeRepository;
    private final UserRepository userRepository;
    private final NoticeAttachmentParseService noticeAttachmentParseService;

    // TODO: application.properties에서 설정으로 빼기
    private static final String UPLOAD_DIR = "/uploads/attachments/";

    /**
     * 사용자 첨부파일 업로드 및 파싱 시작
     * 1. 파일을 스토리지에 저장 (로컬 또는 S3)
     * 2. NoticeFile 엔티티 생성 및 저장
     * 3. NoticeAttachment 엔티티 생성 및 저장
     * 4. 파싱 시작
     */
    @Transactional
    public NoticeAttachment uploadAndParse(
            Long noticeId,
            Long userId,
            MultipartFile file
    ) {
        // 1. 공고 및 사용자 조회
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다: " + noticeId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 2. 파일 저장 (로컬 또는 S3)
        String savedFilePath = saveFile(file);

        // 3. NoticeFile 엔티티 생성 및 저장
        NoticeFile noticeFile = NoticeFile.of(
                notice,
                file.getOriginalFilename(),  // 원본 파일명
                savedFilePath                 // 저장된 경로
        );
        NoticeFile savedNoticeFile = noticeFileRepository.save(noticeFile);

        // 4. NoticeAttachment 엔티티 생성 및 저장
        NoticeAttachment attachment = NoticeAttachment.create(user, savedNoticeFile);
        NoticeAttachment savedAttachment = noticeAttachmentRepository.save(attachment);

        // 5. 비동기 파싱 시작 (또는 동기)
        startParsing(savedAttachment.getAttachmentId(), file);

        return savedAttachment;
    }

    /**
     * 파일 저장 (로컬 스토리지)
     * TODO: S3 등 클라우드 스토리지로 변경 권장
     */
    private String saveFile(MultipartFile file) {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 고유 파일명 생성 (UUID + 원본 확장자)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            return filePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
        }
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

    /**
     * 첨부파일 조회
     */
    @Transactional(readOnly = true)
    public NoticeAttachment getAttachment(Long attachmentId) {
        return noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다: " + attachmentId));
    }

    /* =========================
       상태 관리 메서드
       ========================= */

    @Transactional
    public void markProcessing(Long attachmentId) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다: " + attachmentId));
        attachment.markProcessing();
    }

    @Transactional
    public void completeParsing(Long attachmentId, String parsedJson) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다: " + attachmentId));
        attachment.markDone(parsedJson);
    }

    @Transactional
    public void failParsing(Long attachmentId, String errorMsg) {
        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다: " + attachmentId));
        attachment.markFailed(errorMsg);
    }
}