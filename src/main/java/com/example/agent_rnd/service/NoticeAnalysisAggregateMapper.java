package com.example.agent_rnd.service;

import com.example.agent_rnd.dto.NoticeAnalysisAggregatedResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic mapper for frontend aggregated response.
 *
 * Rules:
 * - Eligibility must come ONLY from checklist_json (no inference from analysis_json)
 * - status/judgment enum output: "가능" | "불가" | "보류"
 * - Keep array order as-is
 */
public final class NoticeAnalysisAggregateMapper {

    private static final Pattern POINTS_PATTERN = Pattern.compile("(\\d+)\\s*점");

    private NoticeAnalysisAggregateMapper() {}

    public static NoticeAnalysisAggregatedResponse from(
            Map<String, Object> analysis,
            Map<String, Object> checklist
    ) {
        Map<String, Object> safeAnalysis = analysis == null ? new LinkedHashMap<>() : analysis;
        Map<String, Object> safeChecklist = checklist == null ? new LinkedHashMap<>() : checklist;

        NoticeAnalysisAggregatedResponse.Eligibility eligibility = mapEligibility(safeChecklist);
        NoticeAnalysisAggregatedResponse.ResearchIntent researchIntent = mapResearchIntent(safeAnalysis);
        NoticeAnalysisAggregatedResponse.EvaluationWeightAnalysis evaluationWeightAnalysis = mapEvaluationWeightAnalysis(safeAnalysis);
        List<String> deliverables = mapDeliverables(safeAnalysis);
        List<String> mandatoryRequirements = mapMandatoryRequirements(safeAnalysis);

        return new NoticeAnalysisAggregatedResponse(
                eligibility,
                researchIntent,
                evaluationWeightAnalysis,
                deliverables,
                mandatoryRequirements
        );
    }

    private static NoticeAnalysisAggregatedResponse.ResearchIntent mapResearchIntent(Map<String, Object> analysis) {
        Map<String, Object> ri = asMap(analysis.get("research_intent"));

        String policyBackground = normalizeWhitespace(pickFirstNonBlank(
                asNullableString(ri.get("policy_background")),
                nestedString(analysis, "background", "summary")
        ));

        List<String> targetIssues = toStringList(pickFirstNonNull(
                ri.get("target_issues"),
                nestedObject(analysis, "background", "key_issues")
        ));

        return new NoticeAnalysisAggregatedResponse.ResearchIntent(
                clampChars(policyBackground, 2000),
                targetIssues
        );
    }

    private static NoticeAnalysisAggregatedResponse.EvaluationWeightAnalysis mapEvaluationWeightAnalysis(Map<String, Object> analysis) {
        Map<String, Object> ewa = asMap(analysis.get("evaluation_weight_analysis"));
        String summary = normalizeWhitespace(asNullableString(ewa.get("summary")));

        Object rawItems = pickFirstNonNull(
                ewa.get("high_weight_items"),
                analysis.get("evaluation_criteria")
        );
        List<NoticeAnalysisAggregatedResponse.HighWeightItem> items = mapHighWeightItems(rawItems);

        if (summary.isBlank()) {
            summary = deriveEvaluationSummary(items);
        }

        return new NoticeAnalysisAggregatedResponse.EvaluationWeightAnalysis(
                clampChars(summary, 800),
                items
        );
    }

    private static String deriveEvaluationSummary(List<NoticeAnalysisAggregatedResponse.HighWeightItem> items) {
        if (items == null || items.isEmpty()) return "";

        int maxPoints = 0;
        String maxTitle = "";
        int total = 0;
        for (NoticeAnalysisAggregatedResponse.HighWeightItem it : items) {
            if (it == null) continue;
            total++;
            if (it.points() > maxPoints) {
                maxPoints = it.points();
                maxTitle = it.item();
            }
        }

        String base = "평가지표는 총 " + total + "개 항목입니다. 고배점 항목 중심으로 전략을 수립하세요.";
        if (maxPoints > 0 && maxTitle != null && !maxTitle.isBlank()) {
            return base + " 최고 배점(" + maxPoints + "점): " + normalizeWhitespace(maxTitle);
        }
        return base;
    }

    private static List<NoticeAnalysisAggregatedResponse.HighWeightItem> mapHighWeightItems(Object raw) {
        List<?> list = asList(raw);
        List<NoticeAnalysisAggregatedResponse.HighWeightItem> out = new ArrayList<>(list.size());

        for (Object it : list) {
            if (!(it instanceof Map<?, ?>)) {
                out.add(new NoticeAnalysisAggregatedResponse.HighWeightItem(
                        normalizeWhitespace(String.valueOf(it)),
                        0,
                        ""
                ));
                continue;
            }

            Map<String, Object> m = asMap(it);
            String item = normalizeWhitespace(pickFirstNonBlank(
                    asNullableString(m.get("item")),
                    asNullableString(m.get("title"))
            ));
            int points = extractPoints(m, item);

            String desc = normalizeWhitespace(pickFirstNonBlank(
                    asNullableString(m.get("strategy")),
                    asNullableString(m.get("description")),
                    joinStrings(toStringList(m.get("perfect_score_strategy"))),
                    asNullableString(m.get("summary"))
            ));
            String strategy = firstSentenceOneLine(desc, 250);

            out.add(new NoticeAnalysisAggregatedResponse.HighWeightItem(
                    item,
                    points,
                    strategy
            ));
        }

        return out;
    }

    private static int extractPoints(Map<String, Object> m, String title) {
        Integer p = tryParseInt(m.get("points"));
        if (p != null) return p;

        Matcher matcher = POINTS_PATTERN.matcher(title == null ? "" : title);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static List<String> mapDeliverables(Map<String, Object> analysis) {
        // Prefer explicit lists from the newer analysis schema
        List<String> explicit = toStringList(pickFirstNonNull(
                analysis.get("deliverables"),
                nestedObject(analysis, "quantitative_targets", "deliverables")
        ));
        if (!explicit.isEmpty()) return explicit;

        // Fallback for legacy analysis schema: split proposal_checklist by doc-ish keywords.
        List<String> proposalChecklist = toStringList(analysis.get("proposal_checklist"));
        if (proposalChecklist.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();
        for (String it : proposalChecklist) {
            String s = normalizeWhitespace(it);
            if (s.isBlank()) continue;
            if (looksLikeDocumentItem(s)) out.add(s);
        }
        return out;
    }

    private static List<String> mapMandatoryRequirements(Map<String, Object> analysis) {
        // Prefer explicit lists from the newer analysis schema
        List<String> explicit = toStringList(pickFirstNonNull(
                analysis.get("mandatory_requirements"),
                nestedObject(analysis, "quantitative_targets", "mandatory_requirements"),
                nestedObject(analysis, "mandatory_vs_optional", "mandatory")
        ));
        if (!explicit.isEmpty()) return explicit;

        // Fallback for legacy analysis schema: proposal_checklist excluding document items.
        List<String> proposalChecklist = toStringList(analysis.get("proposal_checklist"));
        if (proposalChecklist.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();
        for (String it : proposalChecklist) {
            String s = normalizeWhitespace(it);
            if (s.isBlank()) continue;
            if (!looksLikeDocumentItem(s)) out.add(s);
        }
        return out;
    }

    private static boolean looksLikeDocumentItem(String s) {
        String lower = (s == null ? "" : s).toLowerCase(Locale.ROOT);
        return lower.contains("제출")
                || lower.contains("서류")
                || lower.contains("증빙")
                || lower.contains("첨부")
                || lower.contains("양식")
                || lower.contains("신청서")
                || lower.contains("사업자등록")
                || lower.contains("등기")
                || lower.contains("재무제표")
                || lower.contains("증명서")
                || lower.contains("확인서")
                || lower.contains("등록증")
                || lower.contains("서식");
    }

    private static NoticeAnalysisAggregatedResponse.Eligibility mapEligibility(Map<String, Object> checklist) {
        Map<String, Object> overall = asMap(checklist.get("overall_eligibility"));

        String status = normalizeEnum(asNullableString(overall.get("status")));
        String summary = clampToTwoSentences(asNullableString(overall.get("summary")));

        List<NoticeAnalysisAggregatedResponse.EligibilityJudgment> judgments =
                mapJudgments(checklist.get("judgments"));

        List<String> missingInfo = toStringList(checklist.get("missing_info"));
        List<String> warningItems = toStringList(checklist.get("warning_items"));
        List<String> recommendations = toStringList(checklist.get("recommendations"));

        return new NoticeAnalysisAggregatedResponse.Eligibility(
                status,
                summary,
                judgments,
                missingInfo,
                warningItems,
                recommendations
        );
    }

    private static List<NoticeAnalysisAggregatedResponse.EligibilityJudgment> mapJudgments(Object raw) {
        List<?> list = asList(raw);
        List<NoticeAnalysisAggregatedResponse.EligibilityJudgment> out = new ArrayList<>(list.size());

        for (Object it : list) {
            if (!(it instanceof Map<?, ?>)) {
                out.add(new NoticeAnalysisAggregatedResponse.EligibilityJudgment(
                        0,
                        "",
                        normalizeWhitespace(String.valueOf(it)),
                        "보류",
                        "",
                        "",
                        "",
                        null
                ));
                continue;
            }

            Map<String, Object> m = asMap(it);

            int id = Optional.ofNullable(tryParseInt(m.get("id"))).orElse(0);
            String category = normalizeWhitespace(pickFirstNonBlank(
                    asNullableString(m.get("category")),
                    asNullableString(m.get("type")),
                    asNullableString(m.get("section"))
            ));
            String requirementText = normalizeWhitespace(pickFirstNonBlank(
                    asNullableString(m.get("requirement_text")),
                    asNullableString(m.get("requirementText")),
                    asNullableString(m.get("requirement")),
                    asNullableString(m.get("text"))
            ));
            String judgment = normalizeEnum(pickFirstNonBlank(
                    asNullableString(m.get("judgment")),
                    asNullableString(m.get("status")),
                    asNullableString(m.get("result"))
            ));
            String reason = normalizeWhitespace(asNullableString(m.get("reason")));
            String companyInfoUsed = normalizeWhitespace(asNullableString(m.get("company_info_used")));
            String quoteFromAnnouncement = normalizeWhitespace(asNullableString(m.get("quote_from_announcement")));
            String additionalAction = nullableTrimmedString(pickFirstNonBlank(
                    asNullableString(m.get("additional_action")),
                    asNullableString(m.get("additionalAction")),
                    asNullableString(m.get("additional")),
                    asNullableString(m.get("action")),
                    asNullableString(m.get("action_required")),
                    asNullableString(m.get("recommended_action")),
                    asNullableString(m.get("follow_up_action"))
            ));

            out.add(new NoticeAnalysisAggregatedResponse.EligibilityJudgment(
                    id,
                    category,
                    requirementText,
                    judgment,
                    reason,
                    companyInfoUsed,
                    quoteFromAnnouncement,
                    additionalAction
            ));
        }

        return out;
    }

    private static String nestedString(Map<String, Object> root, String k1, String k2) {
        if (root == null) return "";
        Object o1 = root.get(k1);
        if (!(o1 instanceof Map<?, ?>)) return "";
        Map<String, Object> m1 = asMap(o1);
        return asNullableString(m1.get(k2));
    }

    private static Object nestedObject(Map<String, Object> root, String k1, String k2) {
        if (root == null) return null;
        Object o1 = root.get(k1);
        if (!(o1 instanceof Map<?, ?>)) return null;
        Map<String, Object> m1 = asMap(o1);
        return m1.get(k2);
    }

    private static String clampToTwoSentences(String s) {
        String norm = normalizeWhitespace(s);
        if (norm.isBlank()) return "";
        List<String> sentences = splitSentences(norm);
        if (sentences.size() <= 2) return clampChars(norm, 500);
        return clampChars((sentences.get(0) + " " + sentences.get(1)).trim(), 500);
    }

    private static String firstSentenceOneLine(String s, int maxChars) {
        String norm = normalizeWhitespace(s);
        if (norm.isBlank()) return "";
        List<String> sentences = splitSentences(norm);
        String first = sentences.isEmpty() ? norm : sentences.get(0);
        return clampChars(first, maxChars);
    }

    private static List<String> splitSentences(String s) {
        String[] parts = (s == null ? "" : s).split("(?<=[.!?])\\s+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
            if (out.size() >= 10) break;
        }
        if (out.isEmpty() && s != null && !s.isBlank()) out.add(s.trim());
        return out;
    }

    private static String clampChars(String s, int maxChars) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= maxChars) return t;
        int cut = Math.max(0, maxChars - 3);
        return t.substring(0, cut).trim() + "...";
    }

    private static Object pickFirstNonNull(Object... values) {
        if (values == null) return null;
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private static String pickFirstNonBlank(String... values) {
        if (values == null) return "";
        for (String v : values) {
            String s = normalizeWhitespace(v);
            if (!s.isBlank()) return s;
        }
        return "";
    }

    // 가능/불가능/확인 필요 -> 가능/불가/보류 (스펙 고정)
    private static String normalizeEnum(String raw) {
        String s = normalizeWhitespace(raw);
        if (s.isBlank()) return "보류";

        if ("가능".equals(s)) return "가능";
        if ("불가".equals(s)) return "불가";
        if ("보류".equals(s)) return "보류";

        if ("불가능".equals(s)) return "불가";
        if ("확인 필요".equals(s) || "확인필요".equals(s)) return "보류";

        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.contains("불가") || lower.contains("불가능")) return "불가";
        if (lower.contains("확인") || lower.contains("보류")) return "보류";

        return "보류";
    }

    private static String joinStrings(List<String> parts) {
        if (parts == null || parts.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String t = normalizeWhitespace(p);
            if (t.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
            if (sb.length() > 1000) break;
        }
        return sb.toString();
    }

    private static List<String> toStringList(Object raw) {
        List<?> list = asList(raw);
        List<String> out = new ArrayList<>(list.size());
        for (Object v : list) {
            out.add(normalizeWhitespace(asNullableString(v)));
        }
        return out;
    }

    private static List<?> asList(Object o) {
        return (o instanceof List<?> l) ? l : List.of();
    }

    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return new LinkedHashMap<>();
    }

    private static Integer tryParseInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String asNullableString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String nullableTrimmedString(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isBlank() ? null : s;
    }

    private static String normalizeWhitespace(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }
}

