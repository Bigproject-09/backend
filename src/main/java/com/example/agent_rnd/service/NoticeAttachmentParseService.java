package com.example.agent_rnd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class NoticeAttachmentParseService {

    private final RestTemplate restTemplate;

    private static final String FASTAPI_PARSE_URL = "http://localhost:8000/parse";

    /**
     * 기존 공고 첨부 파싱 (기존 동작 유지)
     * - mode=notice로 고정 (FastAPI default가 notice라서 사실상 동일)
     */
    public String parse(MultipartFile file) throws Exception {
        return callFastApi(file, "notice");
    }

    /**
     * 추가: mode 지정 파싱
     * - "sections" 등
     */
    public String parseWithMode(MultipartFile file, String mode) throws Exception {
        return callFastApi(file, mode);
    }

    private String callFastApi(MultipartFile file, String mode) throws Exception {

        String url = UriComponentsBuilder
                .fromHttpUrl(FASTAPI_PARSE_URL)
                .queryParam("mode", mode)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("FastAPI 파싱 실패 (status=" + response.getStatusCode() + ")");
        }
        if (response.getBody() == null || response.getBody().isBlank()) {
            throw new IllegalStateException("FastAPI 파싱 응답이 비어있음");
        }
        return response.getBody();
    }
}
