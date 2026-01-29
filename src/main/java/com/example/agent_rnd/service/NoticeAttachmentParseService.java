package com.example.agent_rnd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
public class NoticeAttachmentParseService {

    private final RestTemplate restTemplate;

    private static final String FASTAPI_PARSE_URL = "http://localhost:8000/parse";

    /**
     * 첨부파일 파싱 요청
     * - 상태 변경은 호출자(NoticeAttachmentService)가 담당
     * - 여기서는 FastAPI 호출 + 결과 반환만 수행
     */
    public String parse(MultipartFile file) throws Exception {
        return callFastApi(file);
    }

    /* =========================
       FastAPI 호출
       ========================= */

    private String callFastApi(MultipartFile file) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        FASTAPI_PARSE_URL,
                        request,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("FastAPI 파싱 실패");
        }

        if (response.getBody() == null || response.getBody().isBlank()) {
            throw new IllegalStateException("FastAPI 파싱 응답이 비어있음");
        }

        return response.getBody();
    }
}
