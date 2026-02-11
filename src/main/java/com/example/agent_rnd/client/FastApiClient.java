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
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    public Map<String, Object> analyzeNotice(Long noticeId, Long companyId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notice_id", noticeId);
        body.put("company_id", companyId == null ? 1 : companyId);
        return postJson("/api/analyze/step1", body);
    }

    public Map<String, Object> searchSimilarRfpV2(Long noticeId, String noticeText, String ministryName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notice_id", noticeId);
        body.put("notice_text", noticeText);
        body.put("ministry_name", ministryName);
        return postJson("/api/analyze/step2", body);
    }

    public Map<String, Object> generatePpt(Long noticeId, MultipartFile file, String bearerToken) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        addMultipartFile(builder, "file", file);
        builder.part("notice_id", String.valueOf(noticeId));

        String token = extractRawToken(bearerToken);
        if (token != null) {
            builder.part("token", token);
        }

        return postMultipart("/api/analyze/step3", builder);
    }

    public Map<String, Object> generateScript(Long noticeId, MultipartFile file, String bearerToken) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        addMultipartFile(builder, "file", file);
        builder.part("notice_id", String.valueOf(noticeId));

        String token = extractRawToken(bearerToken);
        if (token != null) {
            builder.part("token", token);
        }

        return postMultipart("/api/analyze/step4", builder);
    }

    public DownloadedFile downloadGeneratedPpt(String filename) {
        String encodedFilename = UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);

        try {
            return webClient.get()
                    .uri(fastApiBaseUrl + "/download/" + encodedFilename)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(msg -> Mono.error(new IllegalStateException(
                                            "FastAPI download failed: HTTP " + resp.statusCode().value() + " / " + msg
                                    )))
                    )
                    .toEntity(byte[].class)
                    .map(resp -> {
                        byte[] body = resp.getBody() == null ? new byte[0] : resp.getBody();
                        MediaType contentType = resp.getHeaders().getContentType();
                        if (contentType == null) {
                            contentType = MediaType.APPLICATION_OCTET_STREAM;
                        }
                        return new DownloadedFile(body, contentType);
                    })
                    .block();
        } catch (Exception e) {
            log.error("FastAPI generated PPT download failed", e);
            throw new IllegalStateException("FastAPI generated PPT download failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> parseFile(MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            addMultipartFile(builder, "file", file);

            return webClient.post()
                    .uri(fastApiBaseUrl + "/parse")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(msg -> Mono.error(new IllegalStateException(
                                            "FastAPI /parse failed: HTTP " + resp.statusCode().value() + " / " + msg
                                    )))
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("FastAPI /parse call failed", e);
            throw new IllegalStateException("FastAPI /parse call failed: " + e.getMessage(), e);
        }
    }

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
                                            "FastAPI call failed (" + path + "): HTTP " + resp.statusCode().value() + " / " + msg
                                    )))
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("FastAPI call failed: {}", path, e);
            throw new IllegalStateException("FastAPI call failed (" + path + "): " + e.getMessage(), e);
        }
    }

    private Map<String, Object> postMultipart(String path, MultipartBodyBuilder builder) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + path)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(msg -> Mono.error(new IllegalStateException(
                                            "FastAPI call failed (" + path + "): HTTP " + resp.statusCode().value() + " / " + msg
                                    )))
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("FastAPI call failed: {}", path, e);
            throw new IllegalStateException("FastAPI call failed (" + path + "): " + e.getMessage(), e);
        }
    }

    private void addMultipartFile(MultipartBodyBuilder builder, String partName, MultipartFile file) {
        try {
            builder.part(partName, new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
                        }
                    })
                    .contentType(MediaType.APPLICATION_OCTET_STREAM);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build multipart payload: " + e.getMessage(), e);
        }
    }

    private String extractRawToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return null;
        }

        String token = bearerToken.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length()).trim();
        }

        return token.isBlank() ? null : token;
    }

    public record DownloadedFile(byte[] body, MediaType contentType) {}
}
