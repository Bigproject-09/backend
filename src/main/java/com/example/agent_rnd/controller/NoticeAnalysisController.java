package com.example.agent_rnd.controller;

import com.example.agent_rnd.client.FastApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices/{noticeId}")
public class NoticeAnalysisController {

    private final FastApiClient fastApiClient;

    /**
     * [1] 공고문 분석 (자격요건 체크리스트 + 심층 전략 분석)
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeNotice(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        System.out.println("🔍 [Step 1] 공고문 분석 요청 - noticeId: " + noticeId);

        Map<String, Object> result = fastApiClient.analyzeNotice(noticeId, companyId);

        return ResponseEntity.ok(result);
    }

    /**
     * [2] 유관 RFP 검색
     */
    @PostMapping("/search-rfp")
    public ResponseEntity<Map<String, Object>> searchRfp(
            @PathVariable("noticeId") Long noticeId
    ) {
        System.out.println("🔍 [Step 2] 유관 RFP 검색 요청 - noticeId: " + noticeId);

        Map<String, Object> result = fastApiClient.searchSimilarRfp(noticeId);

        return ResponseEntity.ok(result);
    }

    /**
     * [3] PPT 생성
     */
    @PostMapping("/generate-ppt")
    public ResponseEntity<Map<String, Object>> generatePpt(
            @PathVariable("noticeId") Long noticeId
    ) {
        System.out.println("📊 [Step 3] PPT 생성 요청 - noticeId: " + noticeId);

        Map<String, Object> result = fastApiClient.generatePpt(noticeId);

        return ResponseEntity.ok(result);
    }

    /**
     * [4] 스크립트 생성
     */
    @PostMapping("/generate-script")
    public ResponseEntity<Map<String, Object>> generateScript(
            @PathVariable("noticeId") Long noticeId
    ) {
        System.out.println("📝 [Step 4] 스크립트 생성 요청 - noticeId: " + noticeId);

        Map<String, Object> result = fastApiClient.generateScript(noticeId);

        return ResponseEntity.ok(result);
    }
}