package com.example.agent_rnd.service;

import com.example.agent_rnd.client.FastApiClient;
import com.example.agent_rnd.dto.NoticeAnalysisAggregatedResponse;
import com.example.agent_rnd.domain.enums.ReferenceType;
import com.example.agent_rnd.domain.notice.ChecklistItem;
import com.example.agent_rnd.domain.notice.NoticeReference;
import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.repository.ChecklistItemRepository;
import com.example.agent_rnd.repository.NoticeReferenceRepository;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeAnalysisService {

    private final FastApiClient fastApiClient;

    private final ProjectNoticeRepository projectNoticeRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final NoticeReferenceRepository noticeReferenceRepository;

    private final ObjectMapper objectMapper;

    public Map<String, Object> runStep1(Long noticeId, Long companyId) {
        projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 noticeId=" + noticeId));

        Map<String, Object> fastapi = fastApiClient.analyzeNotice(noticeId, companyId);

        Integer savedFromFastApi = null;
        Map<String, Object> data = asMap(fastapi.get("data"));
        Map<String, Object> saved = asMap(data.get("saved"));
        if (!saved.isEmpty()) {
            savedFromFastApi = asInt(saved.get("saved_checklists"));
        }

        long savedFromDb = checklistItemRepository.countByProjectNotice_NoticeId(noticeId);

        return Map.of(
                "status", "success",
                "noticeId", noticeId,
                "savedChecklistCount", (int) savedFromDb,
                "savedChecklistCountReportedByFastApi", savedFromFastApi,
                "fastapi", fastapi
        );
    }

    /**
     * ✅ Step2 (multipart 업로드 기반, Spring 경유 고정)
     * - 프론트 업로드 파일을 FastAPI /parse로 파싱
     * - 파싱 결과로 notice_text 구성 (Service 내부 유틸 사용)
     * - FastAPI /api/analyze/step2에 JSON 호출 (notice_id + notice_text + ministry_name)
     * - 링크(title,url) 추출 후 DB 저장
     */
    public Map<String, Object> runStep2(Long noticeId, Long companyId, MultipartFile file) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 noticeId=" + noticeId));

        // 1) parse
        Map<String, Object> parsed = fastApiClient.parseFile(file);

        // 2) noticeText 만들기 (✅ 존재하는 메서드 사용)
        String noticeText = buildNoticeTextFromParsed(parsed);
        noticeText = clampTextLocal(noticeText, 20000); // ✅ 로컬 제한

        if (noticeText.isBlank()) {
            throw new IllegalStateException("파싱 결과에서 notice_text를 만들지 못했습니다.");
        }

        // 3) ministryName: "소관부처"는 author(예: 해양수산부)를 우선 사용하고,
        //    없으면 집행기관(excInsttNm)으로 fallback.
        String ministryName = Optional.ofNullable(notice.getAuthor()).orElse("").trim();
        if (ministryName.isBlank()) {
            ministryName = Optional.ofNullable(notice.getExcInsttNm()).orElse("").trim();
        }

        // 4) step2 v2
        Map<String, Object> fastapi = fastApiClient.searchSimilarRfpV2(noticeId, noticeText, ministryName);

        // 5) 기존 링크 삭제 후 저장
        noticeReferenceRepository.deleteByProjectNotice_NoticeIdAndType(noticeId, ReferenceType.LINK);

        List<NoticeReference> refs = new ArrayList<>();
        extractUrlTitlePairs(fastapi).stream()
                .limit(30)
                .forEach(p -> refs.add(NoticeReference.of(notice, ReferenceType.LINK, p.title(), p.url())));
        noticeReferenceRepository.saveAll(refs);

        return Map.of(
                "status", "success",
                "noticeId", noticeId,
                "savedReferenceCount", refs.size(),
                "fastapi", fastapi
        );
    }

    public Map<String, Object> runStep3(Long noticeId, Long companyId) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 noticeId=" + noticeId));

        Map<String, Object> fastapi = fastApiClient.generatePpt(noticeId);

        Map<String, Object> data = asMap(fastapi.get("data"));
        String pptPath = asString(data.get("ppt_path"));
        Integer slidesCount = asInt(data.get("slides_count"));

        removeReferencesByTitle(noticeId, "Generated PPT");
        if (pptPath != null && !pptPath.isBlank()) {
            NoticeReference ref = NoticeReference.of(
                    notice,
                    ReferenceType.FILE,
                    "Generated PPT",
                    pptPath
            );
            noticeReferenceRepository.save(ref);
        }

        return Map.of(
                "status", "success",
                "noticeId", noticeId,
                "pptPath", pptPath,
                "slidesCount", slidesCount,
                "fastapi", fastapi
        );
    }

    public Map<String, Object> runStep4(Long noticeId, String bearerToken) {
        Map<String, Object> fastapi = fastApiClient.generateScript(noticeId, bearerToken);

        return Map.of(
                "status", "success",
                "noticeId", noticeId,
                "fastapi", fastapi
        );
    }

    @Transactional(readOnly = true)
    public NoticeAnalysisAggregatedResponse getAggregated(Long noticeId) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("noticeId not found: " + noticeId));

        Map<String, Object> rawAnalysis = parseJsonSafely(notice.getAnalysisJson());
        Map<String, Object> rawChecklist = parseJsonSafely(notice.getChecklistJson());

        return NoticeAnalysisAggregateMapper.from(rawAnalysis, rawChecklist);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStored(Long noticeId) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 noticeId=" + noticeId));

        List<ChecklistItem> checklists = checklistItemRepository.findByProjectNotice_NoticeId(noticeId);
        List<NoticeReference> refs = noticeReferenceRepository.findByProjectNotice_NoticeId(noticeId);

        List<Map<String, Object>> checklistView = new ArrayList<>();
        for (ChecklistItem c : checklists) {
            checklistView.add(Map.of(
                    "checklistId", c.getChecklistId(),
                    "type", c.getType().name(),
                    "content", c.getContent()
            ));
        }

        List<Map<String, Object>> refView = new ArrayList<>();
        for (NoticeReference r : refs) {
            refView.add(Map.of(
                    "referenceId", r.getReferenceId(),
                    "type", r.getType().name(),
                    "title", r.getTitle(),
                    "url", r.getUrl()
            ));
        }

        Map<String, Object> rawChecklist = parseJsonSafely(notice.getChecklistJson());
        Map<String, Object> rawAnalysis = parseJsonSafely(notice.getAnalysisJson());

        Map<String, Object> overall = new LinkedHashMap<>();
        Object o = rawChecklist.get("overall_eligibility");
        if (o instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                overall.put(String.valueOf(e.getKey()), e.getValue());
            }
        }

        return Map.of(
                "noticeId", noticeId,
                "checklists", checklistView,
                "references", refView,
                "raw", Map.of(
                        "checklist", rawChecklist,
                        "analysis", rawAnalysis,
                        "overall_eligibility", overall
                )
        );
    }

    // ----------------- helpers -----------------

    private Map<String, Object> parseJsonSafely(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private void removeReferencesByTitle(Long noticeId, String title) {
        List<NoticeReference> refs = noticeReferenceRepository.findByProjectNotice_NoticeId(noticeId);
        for (NoticeReference r : refs) {
            if (title.equals(r.getTitle())) {
                noticeReferenceRepository.delete(r);
            }
        }
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

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }

    private record Pair(String title, String url) {}

    private static List<Pair> extractUrlTitlePairs(Object root) {
        List<Pair> pairs = new ArrayList<>();
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Object cur = stack.pop();
            if (cur instanceof Map<?, ?> m) {
                Object title = m.get("title");
                Object url = m.get("url");
                if (title != null && url != null) {
                    String t = String.valueOf(title).trim();
                    String u = String.valueOf(url).trim();
                    if (!t.isBlank() && !u.isBlank()) {
                        pairs.add(new Pair(t, u));
                    }
                }
                for (Object v : m.values()) stack.push(v);
            } else if (cur instanceof List<?> list) {
                for (Object v : list) stack.push(v);
            }
        }
        LinkedHashMap<String, Pair> uniq = new LinkedHashMap<>();
        for (Pair p : pairs) {
            uniq.put(p.title() + "|" + p.url(), p);
        }
        return new ArrayList<>(uniq.values());
    }

    // ✅ 여기서 "실제 파싱 결과 구조"를 최대한 안전하게 문자열로 만든다
    private String buildNoticeTextFromParsed(Map<String, Object> parsed) {
        String fileType = String.valueOf(parsed.getOrDefault("file_type", "")).toLowerCase(Locale.ROOT);

        if ("pdf".equals(fileType)) {
            Object pagesObj = parsed.get("pages");
            if (pagesObj instanceof List<?> pages) {
                StringBuilder sb = new StringBuilder();
                for (Object p : pages) {
                    if (p == null) continue;
                    sb.append(String.valueOf(p)).append("\n");
                }
                return sb.toString();
            }
            return "";
        }

        if ("docx".equals(fileType)) {
            Object contentObj = parsed.get("content");
            return contentObj == null ? "" : String.valueOf(contentObj);
        }

        return "";
    }

    private String clampTextLocal(String text, int maxChars) {
        if (text == null) return "";
        String t = text.trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars);
    }
}
