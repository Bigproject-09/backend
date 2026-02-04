//package com.example.agent_rnd.client;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.web.reactive.function.BodyInserters;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Component
//@RequiredArgsConstructor
//public class FastApiClient {
//
//    private final WebClient webClient = WebClient.builder().build();
//
//    @Value("${fastapi.base-url}")
//    private String fastApiBaseUrl;
//
//    /**
//     * FastAPI로 파일 전송 → 파싱 결과(JSON 문자열) 반환
//     */
//    public String parseNoticeAttachment(MultipartFile file) {
//
//        try {
//            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//
//            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
//                @Override
//                public String getFilename() {
//                    return file.getOriginalFilename();
//                }
//            };
//
//            body.add("file", fileResource);
//
//            return webClient.post()
//                    .uri(fastApiBaseUrl + "/parse")
//                    .contentType(MediaType.MULTIPART_FORM_DATA)
//                    .body(BodyInserters.fromMultipartData(body))
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .block();
//
//        } catch (Exception e) {
//            throw new RuntimeException("FastAPI 파싱 요청 실패", e);
//        }
//    }
//}

package com.example.agent_rnd.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;  // http://localhost:8000

    /**
     * [Step 1] 공고문 분석 (체크리스트 + 심층분석)
     */
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

    /**
     * [Step 2] 유관 RFP 검색
     */
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

    /**
     * [Step 3] PPT 생성
     */
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

    /**
     * [Step 4] 스크립트 생성
     */
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