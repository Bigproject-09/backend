package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.NoticeAnalysisAggregatedResponse;
import com.example.agent_rnd.service.AuditLogService;
import com.example.agent_rnd.service.NoticeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/search-rfp")
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

    @PostMapping("/generate-ppt")
    public ResponseEntity<Map<String, Object>> generatePpt(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep3(noticeId, companyId);
        auditLogService.log(userId, "PPT_STEP3", "noticeId=" + noticeId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-script")
    public ResponseEntity<Map<String, Object>> generateScript(
            @PathVariable("noticeId") Long noticeId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal Long userId
    ) {
        String bearer = authHeader;
        Map<String, Object> result = noticeAnalysisService.runStep4(noticeId, bearer);
        auditLogService.log(userId, "SCRIPT_STEP4", "noticeId=" + noticeId);
        return ResponseEntity.ok(result);
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
