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

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient webClient = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs((ClientCodecConfigurer configurer) ->
                            configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) // 20MB
                    )
                    .build())
            .build();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;  // 예: http://localhost:8000

    public String parseBusinessReportSections(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.pdf";
            byte[] bytes = file.getBytes();

            log.info("[FastAPI] baseUrl={}", fastApiBaseUrl);
            log.info("[FastAPI] upload filename={}, size={} bytes", filename, bytes.length);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(bytes) {
                        @Override
                        public String getFilename() {
                            return filename;
                        }
                    })
                    .contentType(MediaType.APPLICATION_OCTET_STREAM);

            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("http")
                            .host(stripSchemeAndPath(fastApiBaseUrl)[0])
                            .port(Integer.parseInt(stripSchemeAndPath(fastApiBaseUrl)[1]))
                            .path("/parse")
                            .queryParam("mode", "sections")
                            .build()
                    )
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchangeToMono(resp ->
                            resp.bodyToMono(byte[].class)
                                    .defaultIfEmpty(new byte[0])
                                    .map(bodyBytes -> {
                                        String body = new String(bodyBytes, StandardCharsets.UTF_8);
                                        int code = resp.statusCode().value();

                                        log.info("[FastAPI] /parse?mode=sections status={}, bodyLen={}", code, bodyBytes.length);

                                        if (code >= 200 && code < 300) return body;

                                        // 여기서 FastAPI가 준 에러 바디를 그대로 노출
                                        throw new RuntimeException("FastAPI /parse 실패: status=" + code + ", body=" + body);
                                    })
                    )
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("FastAPI 사업보고서 섹션 파싱 실패: " + e.getMessage(), e);
        }
    }

    // fastApiBaseUrl이 "http://localhost:8000" 형태라는 전제에서 host/port 뽑는 간단 파서
    private String[] stripSchemeAndPath(String baseUrl) {
        // http://localhost:8000 , http://127.0.0.1:8000 같은 케이스 대응
        String u = baseUrl.trim();
        u = u.replace("http://", "").replace("https://", "");
        // path 제거
        int slash = u.indexOf('/');
        if (slash >= 0) u = u.substring(0, slash);
        String[] hp = u.split(":");
        String host = hp[0];
        String port = (hp.length > 1) ? hp[1] : "8000";
        return new String[]{host, port};
    }

    // 나머지 메서드들은 일단 그대로 둬도 됨 (원인 찾은 뒤 정리)
    public Map<String, Object> analyzeNotice(Long noticeId, Long companyId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step1")
                    .bodyValue(Map.of(
                            "notice_id", noticeId,
                            "company_id", companyId != null ? companyId : 1
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI 공고문 분석 실패", e);
        }
    }

    public Map<String, Object> searchSimilarRfp(Long noticeId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step2")
                    .bodyValue(Map.of("notice_id", noticeId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI RFP 검색 실패", e);
        }
    }

    public Map<String, Object> generatePpt(Long noticeId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step3")
                    .bodyValue(Map.of("notice_id", noticeId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI PPT 생성 실패", e);
        }
    }

    public Map<String, Object> generateScript(Long noticeId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step4")
                    .bodyValue(Map.of("notice_id", noticeId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI 스크립트 생성 실패", e);
        }
    }
}
