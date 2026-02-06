package com.example.agent_rnd.controller;

import com.example.agent_rnd.service.NoticeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices/{noticeId}")
public class NoticeAnalysisController {

    private final NoticeAnalysisService noticeAnalysisService;

    /**
     * [Step 1] 공고문 분석 (자격요건 체크리스트 + 심층 전략 분석)
     * - FastAPI 실행
     * - 체크리스트(요약) DB 저장
     * - FastAPI 원본 응답도 그대로 반환
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeNotice(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep1(noticeId, companyId);
        return ResponseEntity.ok(result);
    }

    /**
     * [Step 2] 유관 RFP 검색
     * - FastAPI 실행
     * - (title,url) 링크를 최대한 추출해서 DB 저장 (notice_references, type=LINK)
     */
    @PostMapping("/search-rfp")
    public ResponseEntity<Map<String, Object>> searchRfp(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep2(noticeId, companyId);
        return ResponseEntity.ok(result);
    }

    /**
     * [Step 3] PPT 생성
     * - FastAPI 실행
     * - 생성된 파일 경로를 DB 저장 (notice_references, type=FILE)
     */
    @PostMapping("/generate-ppt")
    public ResponseEntity<Map<String, Object>> generatePpt(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        Map<String, Object> result = noticeAnalysisService.runStep3(noticeId, companyId);
        return ResponseEntity.ok(result);
    }

    /**
     * [Step 4] 스크립트 생성
     * - FastAPI 실행
     * - (권장) FastAPI가 script_path, qna_count 등을 반환하도록 modeling/main.py도 수정
     */
    @PostMapping("/generate-script")
    public ResponseEntity<Map<String, Object>> generateScript(
            @PathVariable("noticeId") Long noticeId
    ) {
        // Step4는 현재 FastAPI가 noticeId를 받지 않지만, 라우팅 통일 위해 PathVariable 유지
        Map<String, Object> result = noticeAnalysisService.runStep4(noticeId);
        return ResponseEntity.ok(result);
    }

    /**
     * DB에 저장된 결과 조회 (프론트 결과 화면 렌더링용)
     */
    @GetMapping("/analysis-results")
    public ResponseEntity<Map<String, Object>> getStored(
            @PathVariable("noticeId") Long noticeId
    ) {
        return ResponseEntity.ok(noticeAnalysisService.getStored(noticeId));
    }
}
