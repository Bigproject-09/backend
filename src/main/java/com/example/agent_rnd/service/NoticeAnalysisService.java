package com.example.agent_rnd.service;

import com.example.agent_rnd.client.FastApiClient;
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

    /**
     * Step1:
     * - FastAPI 호출
     * - FastAPI가 DB에 저장(project_notices.checklist_json/analysis_json + checklists insert)
     * - Spring은 DB를 다시 읽어서 savedCount를 "확정"해서 반환 (가장 안전)
     */
    public Map<String, Object> runStep1(Long noticeId, Long companyId) {
        projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 noticeId=" + noticeId));

        Map<String, Object> fastapi = fastApiClient.analyzeNotice(noticeId, companyId);

        // ✅ (1) FastAPI 응답에서 saved_checklists 읽기 (키 경로 수정: data.saved.saved_checklists)
        Integer savedFromFastApi = null;
        Map<String, Object> data = asMap(fastapi.get("data"));
        Map<String, Object> saved = asMap(data.get("saved"));
        if (!saved.isEmpty()) {
            savedFromFastApi = asInt(saved.get("saved_checklists"));
        }

        // ✅ (2) DB에서 저장된 체크리스트 개수 확정 (권장)
        long savedFromDb = checklistItemRepository.countByProjectNotice_NoticeId(noticeId);

        return Map.of(
                "status", "success",
                "noticeId", noticeId,
                "savedChecklistCount", (int) savedFromDb,
                "savedChecklistCountReportedByFastApi", savedFromFastApi,
                "fastapi", fastapi
        );
    }

    public Map<String, Object> runStep2(Long noticeId, Long companyId) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 noticeId=" + noticeId));

        Map<String, Object> fastapi = fastApiClient.searchSimilarRfp(noticeId);

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

    public Map<String, Object> runStep4(Long noticeId) {
        Map<String, Object> fastapi = fastApiClient.generateScript(noticeId);
        return Map.of(
                "status", "success",
                "fastapi", fastapi
        );
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
}
