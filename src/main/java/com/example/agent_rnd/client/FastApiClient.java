package com.example.agent_rnd.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;  // 예: http://localhost:8000

    public Map<String, Object> analyzeNotice(Long noticeId, Long companyId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notice_id", noticeId);
        body.put("company_id", companyId == null ? 1 : companyId);
        return postJson("/api/analyze/step1", body);
    }

    // ✅ Step2 v2 (notice_text + ministry_name)
    public Map<String, Object> searchSimilarRfpV2(Long noticeId, String noticeText, String ministryName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notice_id", noticeId);
        body.put("notice_text", noticeText);
        body.put("ministry_name", ministryName);
        return postJson("/api/analyze/step2", body);
    }

    public Map<String, Object> generatePpt(Long noticeId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notice_id", noticeId);
        return postJson("/api/analyze/step3", body);
    }

    public Map<String, Object> generateScript(Long noticeId, String bearerToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notice_id", noticeId);

        WebClient.RequestBodySpec spec = webClient.post()
                .uri(fastApiBaseUrl + "/api/analyze/step4")
                .contentType(MediaType.APPLICATION_JSON);

        // Step4는 FastAPI 쪽이 token을 body로 받는 구조라면 header는 의미 없음.
        // (너 main.py Step4Request는 token 필드 있음) -> 그때는 body에 token 넣어야 함.
        // 일단 "지금 네 FastAPI(main.py)가 header 안 읽는 구조"면 아래 header는 제거하는 게 안전.
        if (bearerToken != null && !bearerToken.isBlank()) {
            spec = spec.header("Authorization", bearerToken);
        }

        return spec
                .bodyValue(body)
                .retrieve()
                .onStatus(s -> s.isError(), resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(msg -> Mono.error(new IllegalStateException(
                                        "FastAPI step4 실패: HTTP " + resp.statusCode().value() + " / " + msg
                                )))
                )
                .bodyToMono(Map.class)
                .block();
    }

    // ✅ /parse (multipart)
    public Map<String, Object> parseFile(MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
                        }
                    })
                    .contentType(MediaType.APPLICATION_OCTET_STREAM);

            return webClient.post()
                    .uri(fastApiBaseUrl + "/parse")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(msg -> Mono.error(new IllegalStateException(
                                            "FastAPI /parse 실패: HTTP " + resp.statusCode().value() + " / " + msg
                                    )))
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("FastAPI /parse 호출 실패", e);
            throw new IllegalStateException("FastAPI /parse 호출 실패: " + e.getMessage(), e);
        }
    }

    // ---------------- helper ----------------

    private Map<String, Object> postJson(String path, Map<String, Object> body) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(msg -> Mono.error(new IllegalStateException(
                                            "FastAPI 호출 실패(" + path + "): HTTP " + resp.statusCode().value() + " / " + msg
                                    )))
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("FastAPI 호출 실패: {}", path, e);
            throw new IllegalStateException("FastAPI 호출 실패(" + path + "): " + e.getMessage(), e);
        }
    }
    private String clampText(String s, int maxLen) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= maxLen) return t;
        return t.substring(0, maxLen);
    }

    private String buildNoticeTextFromParseResult(Map<String, Object> parsed) {
        if (parsed == null) return "";

        Object ft = parsed.get("file_type");
        String fileType = ft == null ? "" : String.valueOf(ft).toLowerCase();

        if ("pdf".equals(fileType)) {
            Object pagesObj = parsed.get("pages");
            if (pagesObj instanceof java.util.List<?> pages) {
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
}
