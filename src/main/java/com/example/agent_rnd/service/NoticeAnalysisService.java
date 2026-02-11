package com.example.agent_rnd.service;

import com.example.agent_rnd.client.FastApiClient;
import com.example.agent_rnd.domain.enums.ReferenceType;
import com.example.agent_rnd.domain.notice.ChecklistItem;
import com.example.agent_rnd.domain.notice.NoticeReference;
import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.dto.NoticeAnalysisAggregatedResponse;
import com.example.agent_rnd.repository.ChecklistItemRepository;
import com.example.agent_rnd.repository.NoticeReferenceRepository;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                .orElseThrow(() -> new IllegalArgumentException("noticeId not found: " + noticeId));

        Map<String, Object> fastapi = fastApiClient.analyzeNotice(noticeId, companyId);

        Integer savedFromFastApi = null;
        Map<String, Object> data = asMap(fastapi.get("data"));
        Map<String, Object> saved = asMap(data.get("saved"));
        if (!saved.isEmpty()) {
            savedFromFastApi = asInt(saved.get("saved_checklists"));
        }

        long savedFromDb = checklistItemRepository.countByProjectNotice_NoticeId(noticeId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("noticeId", noticeId);
        response.put("savedChecklistCount", (int) savedFromDb);
        response.put("savedChecklistCountReportedByFastApi", savedFromFastApi);
        response.put("fastapi", fastapi);
        return response;
    }

    public Map<String, Object> runStep2(Long noticeId, Long companyId, MultipartFile file) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("noticeId not found: " + noticeId));

        Map<String, Object> parsed = fastApiClient.parseFile(file);

        String noticeText = buildNoticeTextFromParsed(parsed);
        noticeText = clampTextLocal(noticeText, 20000);
        if (noticeText.isBlank()) {
            throw new IllegalStateException("Could not build notice_text from parse output");
        }

        String ministryName = Optional.ofNullable(notice.getAuthor()).orElse("").trim();
        if (ministryName.isBlank()) {
            ministryName = Optional.ofNullable(notice.getExcInsttNm()).orElse("").trim();
        }

        Map<String, Object> fastapi = fastApiClient.searchSimilarRfpV2(noticeId, noticeText, ministryName);

        noticeReferenceRepository.deleteByProjectNotice_NoticeIdAndType(noticeId, ReferenceType.LINK);

        List<NoticeReference> refs = new ArrayList<>();
        extractUrlTitlePairs(fastapi).stream()
                .limit(30)
                .forEach(p -> refs.add(NoticeReference.of(notice, ReferenceType.LINK, p.title(), p.url())));
        noticeReferenceRepository.saveAll(refs);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("noticeId", noticeId);
        response.put("savedReferenceCount", refs.size());
        response.put("fastapi", fastapi);
        return response;
    }

    public Map<String, Object> runStep3(Long noticeId, Long companyId, MultipartFile file, String bearerToken) {
        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("noticeId not found: " + noticeId));

        Map<String, Object> fastapi = fastApiClient.generatePpt(noticeId, file, bearerToken);
        Map<String, Object> data = asMap(fastapi.get("data"));

        String pptFilename = asString(data.get("pptx_filename"));
        String pptPath = asString(data.get("pptx_path"));
        if (pptPath.isBlank()) {
            pptPath = asString(data.get("ppt_path"));
        }

        Integer slidesCount = asInt(data.get("total_slides"));
        if (slidesCount == null) {
            slidesCount = asInt(data.get("slides_count"));
        }

        String downloadPath = "";
        if (!pptFilename.isBlank()) {
            downloadPath = "/api/notices/" + noticeId + "/generated-ppt/download?filename=" + urlEncode(pptFilename);
        }

        removeReferencesByTitle(noticeId, "Generated PPT");
        String referencePath = !downloadPath.isBlank() ? downloadPath : pptPath;
        if (!referencePath.isBlank()) {
            NoticeReference ref = NoticeReference.of(notice, ReferenceType.FILE, "Generated PPT", referencePath);
            noticeReferenceRepository.save(ref);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("noticeId", noticeId);
        response.put("pptPath", pptPath);
        response.put("pptxFilename", pptFilename);
        response.put("slidesCount", slidesCount);
        response.put("downloadPath", downloadPath);
        response.put("data", data);
        response.put("fastapi", fastapi);
        return response;
    }

    public Map<String, Object> runStep4(Long noticeId, MultipartFile file, String bearerToken) {
        projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("noticeId not found: " + noticeId));

        Map<String, Object> fastapi = fastApiClient.generateScript(noticeId, file, bearerToken);
        Map<String, Object> data = asMap(fastapi.get("data"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("noticeId", noticeId);
        response.put("data", data);
        response.put("fastapi", fastapi);
        return response;
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadGeneratedPpt(Long noticeId, String filename) {
        projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("noticeId not found: " + noticeId));

        FastApiClient.DownloadedFile remote = fastApiClient.downloadGeneratedPpt(filename);
        return new DownloadPayload(filename, remote.body(), remote.contentType());
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
                .orElseThrow(() -> new IllegalArgumentException("noticeId not found: " + noticeId));

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
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }

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
        if (o == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
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
                for (Object v : m.values()) {
                    stack.push(v);
                }
            } else if (cur instanceof List<?> list) {
                for (Object v : list) {
                    stack.push(v);
                }
            }
        }

        Set<String> seen = new LinkedHashSet<>();
        List<Pair> uniq = new ArrayList<>();
        for (Pair p : pairs) {
            String key = p.title() + "|" + p.url();
            if (seen.add(key)) {
                uniq.add(p);
            }
        }
        return uniq;
    }

    private String buildNoticeTextFromParsed(Map<String, Object> parsed) {
        String fileType = String.valueOf(parsed.getOrDefault("file_type", "")).toLowerCase(Locale.ROOT);

        if ("pdf".equals(fileType)) {
            Object pagesObj = parsed.get("pages");
            if (pagesObj instanceof List<?> pages) {
                StringBuilder sb = new StringBuilder();
                for (Object p : pages) {
                    if (p == null) {
                        continue;
                    }
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
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, maxChars);
    }

    private String urlEncode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    public record DownloadPayload(String filename, byte[] body, MediaType contentType) {}
}
