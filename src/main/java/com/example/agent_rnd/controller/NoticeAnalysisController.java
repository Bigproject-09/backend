package com.example.agent_rnd.controller;

import com.example.agent_rnd.service.AuditLogService;
import com.example.agent_rnd.dto.NoticeAnalysisAggregatedResponse;
import com.example.agent_rnd.service.NoticeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    private final AuditLogService auditLogService; // ✅ 추가

    /**
     * [Step 1] 공고문 분석
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeNotice(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @AuthenticationPrincipal Long userId // ✅ 추가
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep1(noticeId, companyId);

        // ✅ Step1 로그
        auditLogService.log(userId, "ANALYZE_STEP1", "noticeId=" + noticeId);

        return ResponseEntity.ok(result);
    }

    /**
     * [Step 2] 유관 RFP 검색 (✅ multipart 업로드 기반)
     * - 프론트에서 공고문 파일 업로드(필수)
     * - Spring이 FastAPI /parse로 파싱 → notice_text 구성
     * - FastAPI /api/analyze/step2로 JSON 호출
     * - (title,url) 링크를 추출하여 DB 저장 (notice_references, type=LINK)
     */
    @PostMapping("/search-rfp")
    public ResponseEntity<Map<String, Object>> searchRfp(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId // ✅ 추가
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep2(noticeId, companyId, file);

        // ✅ Step2 로그 (파일명 같이 저장 추천)
        String target = "noticeId=" + noticeId + ", file=" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "-");
        auditLogService.log(userId, "SEARCH_STEP2", target);

        return ResponseEntity.ok(result);
    }


    /**
     * [Step 3] PPT 생성
     */
    @PostMapping("/generate-ppt")
    public ResponseEntity<Map<String, Object>> generatePpt(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @AuthenticationPrincipal Long userId // ✅ 추가
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep3(noticeId, companyId);

        // ✅ Step3 로그
        auditLogService.log(userId, "PPT_STEP3", "noticeId=" + noticeId);

        return ResponseEntity.ok(result);
    }

    /**
     * [Step 4] 스크립트 생성
     */
    @PostMapping("/generate-script")
    public ResponseEntity<Map<String, Object>> generateScript(
            @PathVariable("noticeId") Long noticeId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal Long userId // ✅ 추가
    ) {
        String bearer = authHeader; // "Bearer xxx" 그대로 넘김
        Map<String, Object> result = noticeAnalysisService.runStep4(noticeId, bearer);

        // ✅ Step4 로그
        auditLogService.log(userId, "SCRIPT_STEP4", "noticeId=" + noticeId);

        return ResponseEntity.ok(result);
    }

    /**
     * DB에 저장된 결과 조회
     */
    @GetMapping("/analysis-results")
    public ResponseEntity<Map<String, Object>> getStored(
            @PathVariable("noticeId") Long noticeId
    ) {
        return ResponseEntity.ok(noticeAnalysisService.getStored(noticeId));
    }

    /**
     * 프론트 전용: analysis_json + checklist_json 집계 응답
     */
    @GetMapping("/analysis-aggregated")
    public ResponseEntity<NoticeAnalysisAggregatedResponse> getAggregated(
            @PathVariable("noticeId") Long noticeId
    ) {
        return ResponseEntity.ok(noticeAnalysisService.getAggregated(noticeId));
    }
}
