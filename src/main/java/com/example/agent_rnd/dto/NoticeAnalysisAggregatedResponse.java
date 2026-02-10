package com.example.agent_rnd.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Frontend-only aggregated response built from DB JSON columns:
 * - project_notices.analysis_json
 * - project_notices.checklist_json
 *
 * This DTO is intentionally shaped for the Result UI sections:
 * 1) 자격요건 체크리스트 (eligibility)
 * 2) 과제 의도 및 목적 (research_intent)
 * 3) 평가지표 분석 (evaluation_weight_analysis)
 * 4) 제출 문서 리스트 (deliverables)
 * 5) 필수 준수사항 (mandatory_requirements)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record NoticeAnalysisAggregatedResponse(
        @JsonProperty("eligibility")
        Eligibility eligibility,

        @JsonProperty("research_intent")
        ResearchIntent researchIntent,

        @JsonProperty("evaluation_weight_analysis")
        EvaluationWeightAnalysis evaluationWeightAnalysis,

        @JsonProperty("deliverables")
        List<String> deliverables,

        @JsonProperty("mandatory_requirements")
        List<String> mandatoryRequirements
) {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ResearchIntent(
            @JsonProperty("policy_background")
            String policyBackground,

            @JsonProperty("target_issues")
            List<String> targetIssues
    ) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record EvaluationWeightAnalysis(
            @JsonProperty("summary")
            String summary,

            @JsonProperty("high_weight_items")
            List<HighWeightItem> highWeightItems
    ) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record HighWeightItem(
            @JsonProperty("item")
            String item,

            @JsonProperty("points")
            int points,

            @JsonProperty("strategy")
            String strategy
    ) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Eligibility(
            @JsonProperty("status")
            String status,

            @JsonProperty("summary")
            String summary,

            @JsonProperty("judgments")
            List<EligibilityJudgment> judgments,

            @JsonProperty("missing_info")
            List<String> missingInfo,

            @JsonProperty("warning_items")
            List<String> warningItems,

            @JsonProperty("recommendations")
            List<String> recommendations
    ) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record EligibilityJudgment(
            @JsonProperty("id")
            int id,

            @JsonProperty("category")
            String category,

            @JsonProperty("requirement_text")
            String requirementText,

            @JsonProperty("judgment")
            String judgment,

            @JsonProperty("reason")
            String reason,

            @JsonProperty("company_info_used")
            String companyInfoUsed,

            @JsonProperty("quote_from_announcement")
            String quoteFromAnnouncement,

            @JsonProperty("additional_action")
            String additionalAction
    ) {}
}
