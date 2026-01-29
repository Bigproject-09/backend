package com.example.agent_rnd.controller;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import com.example.agent_rnd.dto.NoticeDetailResponse;
import com.example.agent_rnd.dto.NoticeListResponse;
import com.example.agent_rnd.service.ProjectNoticeService;
import com.example.agent_rnd.service.NoticeAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class ProjectNoticeController {

    private final ProjectNoticeService projectNoticeService;
    private final NoticeAttachmentService noticeAttachmentService;

    /**
     * 공고 목록 조회
     */
    @GetMapping
    public Page<NoticeListResponse> getNotices(
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable
    ) {
        return projectNoticeService.getNoticeList(pageable);
    }

    /**
     * 공고 상세 조회
     */
    @GetMapping("/{id}")
    public NoticeDetailResponse getNotice(@PathVariable Long id) {
        return projectNoticeService.getNoticeDetail(id);
    }

    /**
     * ✅ 기업마당 원본 첨부파일 다운로드
     * (PROJECT_NOTICES 기준)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadNoticeFile(
            @PathVariable Long id
    ) {
        return projectNoticeService.downloadNoticeFile(id);
    }

    /**
     * 사용자 첨부파일 업로드 (파싱 전)
     */
    @PostMapping("/{id}/attachments")
    public ResponseEntity<Long> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {
        NoticeAttachment attachment =
                noticeAttachmentService.upload(id, userId, file);

        return ResponseEntity.ok(attachment.getId());
    }

    /**
     * 기업마당 기술공고 수집 트리거
     */
    @PostMapping("/collect")
    public ResponseEntity<String> collectNotices() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "C:/Users/User/Desktop/API/document_api.py"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            return ResponseEntity.ok("기업마당 기술공고 수집 완료");

        } catch (Exception e) {
            throw new RuntimeException("공고 수집 실패", e);
        }
    }
}
