package com.example.agent_rnd.service;

import com.example.agent_rnd.dto.NoticeAnalysisAggregatedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoticeAnalysisAggregateMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsLegacyAnalysisSchemaToFiveSections() throws Exception {
        String analysisJson = """
                {
                  "background": {
                    "summary": "본 사업은 공공 부문의 혁신 수요를 기반으로, 중소기업의 혁신적인 제품 및 기술을 발굴하고 공공 서비스에 적용하는 것을 목표로 합니다.",
                    "key_issues": ["이슈A", "이슈B"]
                  },
                  "proposal_checklist": [
                    "제출 서류 목록(사업자등록증 등)을 모두 확인하고 누락 없이 준비되었는가?",
                    "사업 배경/목적이 공고문의 정책 방향과 일치하는가?"
                  ],
                  "evaluation_criteria": [
                    {
                      "title": "과제 목표 및 내용의 혁신성 및 공공성 (30점)",
                      "points": 30,
                      "description": "제안 기술/제품이 해결하고자 하는 공공 문제의 명확성, 기술적 혁신성, 그리고 공공 서비스 개선 및 국민 삶의 질 향상에 기여하는 정도를 평가합니다."
                    },
                    {
                      "title": "Scale-up 및 사업화 계획의 구체성 및 실현 가능성 (30점)",
                      "points": 30,
                      "description": "제안 기술/제품이 초기 개발 단계를 넘어 실제 공공 시장으로 확장될 수 있는 구체적인 전략, 시장 분석, 재원 확보 방안 및 사업화 목표의 실현 가능성을 평가합니다."
                    }
                  ]
                }
                """;

        String checklistJson = """
                {
                  "overall_eligibility": { "status": "가능", "summary": "요건 충족. 따라서 가능." },
                  "judgments": [
                    {
                      "id": 1,
                      "category": "신청주체 유형",
                      "judgment": "확인 필요",
                      "requirement_text": "중소기업",
                      "reason": "근거",
                      "additional_action": null,
                      "company_info_used": "회사정보",
                      "quote_from_announcement": "공고문 인용"
                    }
                  ],
                  "missing_info": [],
                  "warning_items": [],
                  "recommendations": ["권장"]
                }
                """;

        Map<String, Object> analysis = objectMapper.readValue(analysisJson, Map.class);
        Map<String, Object> checklist = objectMapper.readValue(checklistJson, Map.class);

        NoticeAnalysisAggregatedResponse resp = NoticeAnalysisAggregateMapper.from(analysis, checklist);

        // 1) eligibility
        assertEquals("가능", resp.eligibility().status());
        assertEquals(1, resp.eligibility().judgments().get(0).id());
        assertEquals("보류", resp.eligibility().judgments().get(0).judgment()); // 확인 필요 -> 보류
        assertNull(resp.eligibility().judgments().get(0).additionalAction());

        // 2) research_intent (fallback from background)
        assertFalse(resp.researchIntent().policyBackground().isBlank());
        assertEquals(List.of("이슈A", "이슈B"), resp.researchIntent().targetIssues());

        // 3) evaluation_weight_analysis (fallback from evaluation_criteria)
        assertEquals(2, resp.evaluationWeightAnalysis().highWeightItems().size());
        assertEquals(30, resp.evaluationWeightAnalysis().highWeightItems().get(0).points());
        assertFalse(resp.evaluationWeightAnalysis().highWeightItems().get(0).strategy().isBlank());

        // 4) deliverables (filtered from proposal_checklist)
        assertEquals(1, resp.deliverables().size());

        // 5) mandatory_requirements (proposal_checklist excluding deliverables)
        assertEquals(1, resp.mandatoryRequirements().size());
    }

    @Test
    void normalizesEnums() {
        Map<String, Object> analysis = Map.of();
        Map<String, Object> checklist = Map.of(
                "overall_eligibility", Map.of("status", "확인 필요", "summary", ""),
                "judgments", List.of(Map.of("id", 1, "judgment", "불가능"))
        );

        NoticeAnalysisAggregatedResponse resp = NoticeAnalysisAggregateMapper.from(analysis, checklist);
        assertEquals("보류", resp.eligibility().status());
        assertEquals("불가", resp.eligibility().judgments().get(0).judgment());
    }

    @Test
    void mapsNewAnalysisSchemaDirectly() throws Exception {
        String analysisJson = """
                {
                  "title": "공고문 실무 분석 리포트",
                  "research_intent": {
                    "policy_background": "본 사업은 극한지 탐사 역량을 강화하기 위한 로봇-ICT 융합기술 개발을 목표로 합니다. 현장 적용 가능성과 확산을 위해 실증이 중요합니다."
                    ,"target_issues": ["T1", "T2"]
                  },
                  "evaluation_weight_analysis": {
                    "summary": "요약",
                    "high_weight_items": [
                      { "item": "기술의 혁신성 및 우수성", "points": 30, "strategy": "차별화된 핵심기술을 명확히 제시합니다. 정량 지표를 포함합니다." },
                      { "item": "사업화 계획", "points": 20, "strategy": "현장 실증과 확산 계획을 구체화합니다." }
                    ]
                  },
                  "quantitative_targets": {
                    "deliverables": ["D1", "D2"],
                    "mandatory_requirements": ["M1", "M2"]
                  }
                }
                """;

        String checklistJson = """
                {
                  "overall_eligibility": { "status": "불가능", "summary": "요건 미충족" },
                  "judgments": [],
                  "missing_info": [],
                  "warning_items": [],
                  "recommendations": []
                }
                """;

        Map<String, Object> analysis = objectMapper.readValue(analysisJson, Map.class);
        Map<String, Object> checklist = objectMapper.readValue(checklistJson, Map.class);

        NoticeAnalysisAggregatedResponse resp = NoticeAnalysisAggregateMapper.from(analysis, checklist);

        assertEquals("본 사업은 극한지 탐사 역량을 강화하기 위한 로봇-ICT 융합기술 개발을 목표로 합니다. 현장 적용 가능성과 확산을 위해 실증이 중요합니다.", resp.researchIntent().policyBackground());
        assertEquals(List.of("T1", "T2"), resp.researchIntent().targetIssues());

        assertEquals(2, resp.evaluationWeightAnalysis().highWeightItems().size());
        assertEquals("기술의 혁신성 및 우수성", resp.evaluationWeightAnalysis().highWeightItems().get(0).item());
        assertEquals(30, resp.evaluationWeightAnalysis().highWeightItems().get(0).points());
        assertFalse(resp.evaluationWeightAnalysis().highWeightItems().get(0).strategy().isBlank());

        assertEquals(List.of("D1", "D2"), resp.deliverables());
        assertEquals(List.of("M1", "M2"), resp.mandatoryRequirements());

        assertEquals("불가", resp.eligibility().status());
    }
}
