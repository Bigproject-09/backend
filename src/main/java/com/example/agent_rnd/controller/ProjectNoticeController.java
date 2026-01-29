package com.example.agent_rnd.controller;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import com.example.agent_rnd.dto.NoticeDetailResponse;
import com.example.agent_rnd.dto.NoticeListResponse;
import com.example.agent_rnd.service.NoticeAttachmentService;
import com.example.agent_rnd.service.ProjectNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class ProjectNoticeController {

    private final ProjectNoticeService projectNoticeService;
    private final NoticeAttachmentService noticeAttachmentService;
    private final RestTemplate restTemplate;

    /**
     * 공고 목록 조회
     */
    @GetMapping
    public Page<NoticeListResponse> getNotices(
            @PageableDefault(size = 10, sort = "noticeId")
            Pageable pageable
    ) {
        return projectNoticeService.getNoticeList(pageable);
    }

    /**
     * 공고 상세 조회
     */
    @GetMapping("/{id}")
    public NoticeDetailResponse getNotice(@PathVariable("id") Long noticeId) {
        return projectNoticeService.getNoticeDetail(noticeId);
    }

    /**
     * 기업마당 원본 첨부파일 다운로드
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadNoticeFile(
            @PathVariable("id") Long noticeId
    ) {
        return projectNoticeService.downloadNoticeFile(noticeId);
    }

    /**
     * 사용자 첨부파일 업로드 (파싱 전)
     */
    @PostMapping("/{id}/attachments")
    public ResponseEntity<Long> uploadAttachment(
            @PathVariable("id") Long noticeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {
        NoticeAttachment attachment =
                noticeAttachmentService.upload(noticeId, userId, file);

        return ResponseEntity.ok(attachment.getAttachmentId());
    }

    /**
     * 기업마당 기술공고 수집 트리거
     * (FastAPI 위임 + 결과만 반환)
     */
    @PostMapping("/collect")
    public ResponseEntity<?> collectNotices() {
        String fastApiUrl = "http://localhost:8000/collect/notices";
        return restTemplate.postForEntity(fastApiUrl, null, Object.class);
    }
}
