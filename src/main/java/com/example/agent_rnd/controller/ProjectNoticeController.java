package com.example.agent_rnd.controller;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import com.example.agent_rnd.dto.NoticeDetailResponse;
import com.example.agent_rnd.dto.NoticeListResponse;
import com.example.agent_rnd.service.NoticeAttachmentService;
import com.example.agent_rnd.service.NoticeFileService;
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
import lombok.Getter;
import lombok.AllArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class ProjectNoticeController {

    private final ProjectNoticeService projectNoticeService;
    private final NoticeFileService noticeFileService;
    private final NoticeAttachmentService noticeAttachmentService;
    private final RestTemplate restTemplate;

    /**
     * 공고 목록 조회 (페이징)
     */
    @GetMapping
    public Page<NoticeListResponse> getNotices(
            @PageableDefault(size = 10, sort = "noticeId")
            Pageable pageable
    ) {
        return projectNoticeService.getNoticeList(pageable);
    }

    /**
     * 기업마당 기술공고 수집 트리거
     */
    @PostMapping("/collect")
    public ResponseEntity<?> collectNotices() {
        System.out.println("✅ /collect 호출됨!");
        String fastApiUrl = "http://localhost:8000/collect/notices";
        return restTemplate.postForEntity(fastApiUrl, null, Object.class);
    }

    /**
     * 공고 상세 조회
     * ✅ 정규식 추가: 숫자만 받음
     */
    @GetMapping("/{id:[0-9]+}")
    public NoticeDetailResponse getNotice(@PathVariable("id") Long noticeId) {
        return projectNoticeService.getNoticeDetail(noticeId);
    }

    /**
     * 공고 파일 다운로드
     * ✅ 정규식 추가: 숫자만 받음
     */
    @GetMapping("/{noticeId:[0-9]+}/files/{fileId:[0-9]+}/download")
    public ResponseEntity<InputStreamResource> downloadNoticeFile(
            @PathVariable("noticeId") Long noticeId,
            @PathVariable("fileId") Long fileId
    ) {
        return noticeFileService.downloadFile(noticeId, fileId);
    }

    /**
     * 사용자 첨부파일 업로드 및 파싱 요청
     * ✅ 정규식 추가: 숫자만 받음
     */
    @PostMapping("/{id:[0-9]+}/attachments")
    public ResponseEntity<Long> uploadAttachment(
            @PathVariable("id") Long noticeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {
        NoticeAttachment attachment =
                noticeAttachmentService.uploadAndParse(noticeId, userId, file);

        return ResponseEntity.ok(attachment.getAttachmentId());
    }

    /**
     * 첨부파일 파싱 상태 조회
     * ✅ 정규식 추가: 숫자만 받음
     */
    @GetMapping("/attachments/{attachmentId:[0-9]+}/status")
    public ResponseEntity<?> getParseStatus(
            @PathVariable("attachmentId") Long attachmentId
    ) {
        NoticeAttachment attachment =
                noticeAttachmentService.getAttachment(attachmentId);

        return ResponseEntity.ok()
                .body(new ParseStatusResponse(
                        attachment.getAttachmentId(),
                        attachment.getParseStatus(),
                        attachment.getErrorMsg()
                ));
    }

    @Getter
    @AllArgsConstructor
    private static class ParseStatusResponse {
        private Long attachmentId;
        private NoticeAttachment.ParseStatus status;
        private String errorMsg;
    }
}