package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.NoticeAnalysisAggregatedResponse;
import com.example.agent_rnd.service.AuditLogService;
import com.example.agent_rnd.service.NoticeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices/{noticeId}")
public class NoticeAnalysisController {

    private final NoticeAnalysisService noticeAnalysisService;
    private final AuditLogService auditLogService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeNotice(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep1(noticeId, companyId);
        auditLogService.log(userId, "ANALYZE_STEP1", "noticeId=" + noticeId);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/search-rfp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> searchRfp(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep2(noticeId, companyId, file);
        String target = "noticeId=" + noticeId + ", file=" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "-");
        auditLogService.log(userId, "SEARCH_STEP2", target);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/generate-ppt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> generatePpt(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep3(noticeId, companyId, file, authHeader);
        auditLogService.log(userId, "PPT_STEP3", "noticeId=" + noticeId);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/generate-script", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> generateScript(
            @PathVariable("noticeId") Long noticeId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep4(noticeId, file, authHeader);
        auditLogService.log(userId, "SCRIPT_STEP4", "noticeId=" + noticeId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/generated-ppt/download")
    public ResponseEntity<byte[]> downloadGeneratedPpt(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam("filename") String filename
    ) {
        NoticeAnalysisService.DownloadPayload payload = noticeAnalysisService.downloadGeneratedPpt(noticeId, filename);

        return ResponseEntity.ok()
                .contentType(payload.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.filename() + "\"")
                .body(payload.body());
    }

    @GetMapping("/analysis-results")
    public ResponseEntity<Map<String, Object>> getStored(
            @PathVariable("noticeId") Long noticeId
    ) {
        return ResponseEntity.ok(noticeAnalysisService.getStored(noticeId));
    }

    @GetMapping("/analysis-aggregated")
    public ResponseEntity<NoticeAnalysisAggregatedResponse> getAggregated(
            @PathVariable("noticeId") Long noticeId
    ) {
        return ResponseEntity.ok(noticeAnalysisService.getAggregated(noticeId));
    }
}
