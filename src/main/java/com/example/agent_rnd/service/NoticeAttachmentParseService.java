package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import com.example.agent_rnd.repository.NoticeAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
public class NoticeAttachmentParseService {

    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // ⚠️ FastAPI 주소 (환경 맞게 수정 가능)
    private static final String FASTAPI_PARSE_URL = "http://localhost:8000/parse";

    /**
     * 업로드된 첨부파일 → FastAPI 파싱 → DB 저장
     */
    @Transactional
    public void parseAndSave(
            Long attachmentId,
            MultipartFile file
    ) {

        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일 없음"));

        try {
            // 1️⃣ 상태 변경
            attachment.markProcessing();

            // 2️⃣ FastAPI로 multipart 전송
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

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("FastAPI 파싱 실패");
            }

            // 3️⃣ 파싱 결과 저장
            attachment.markDone(response.getBody());

        } catch (Exception e) {
            // 4️⃣ 실패 처리
            attachment.markFailed(e.getMessage());
            throw new RuntimeException("첨부파일 파싱 실패", e);
        }
    }
}
