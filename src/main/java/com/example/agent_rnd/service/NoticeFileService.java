package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.NoticeFile;
import com.example.agent_rnd.repository.NoticeFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeFileService {

    private final NoticeFileRepository noticeFileRepository;
    private final RestTemplate restTemplate;

    /**
     * 공고 파일 목록 조회
     */
    public List<NoticeFile> getFilesByNoticeId(Long noticeId) {
        return noticeFileRepository.findByProjectNotice_NoticeId(noticeId);
    }

    /**
     * 파일 단건 조회
     */
    public NoticeFile getFile(Long fileId) {
        return noticeFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));
    }

    /**
     * 기업마당 공고 파일 다운로드
     * (notice_files 테이블의 print_flpth_nm + print_file_nm 사용)
     *
     * @param noticeId 공고 ID
     * @param fileId 파일 ID
     * @return 다운로드 가능한 파일 리소스
     */
    public ResponseEntity<InputStreamResource> downloadFile(Long noticeId, Long fileId) {

        // 1. 파일 정보 조회
        NoticeFile noticeFile = noticeFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));

        // 2. 해당 공고의 파일인지 검증
        if (!noticeFile.getProjectNotice().getNoticeId().equals(noticeId)) {
            throw new IllegalArgumentException("해당 공고의 파일이 아닙니다");
        }

        String filePath = noticeFile.getPrintFlpthNm();   // ex) /uss/file/download/xxx
        String fileName = noticeFile.getPrintFileNm();    // ex) 공고문.pdf

        if (filePath == null || fileName == null) {
            throw new IllegalStateException("파일 경로 정보가 없습니다");
        }

        // 3. 다운로드 URL 구성
        String downloadUrl = buildDownloadUrl(filePath);

        // 4. 외부 파일 다운로드 및 반환
        return downloadFromExternalUrl(downloadUrl, fileName);
    }

    /**
     * 다운로드 URL 구성
     */
    private String buildDownloadUrl(String path) {
        if (path.startsWith("http")) {
            // 이미 전체 URL인 경우
            return path;
        } else {
            // 상대경로인 경우 도메인 붙이기
            return "https://www.bizinfo.go.kr" + path;
        }
    }

    /**
     * 외부 URL에서 파일 다운로드
     */
    private ResponseEntity<InputStreamResource> downloadFromExternalUrl(String url, String fileName) {
        try {
            log.info("파일 다운로드 시작: {} from {}", fileName, url);

            // 1. HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.setAccept(MediaType.parseMediaTypes("*/*"));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 2. 외부 URL 요청
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            // 3. 응답 검증
            if (response.getBody() == null || response.getBody().length == 0) {
                throw new RuntimeException("다운로드 결과가 비어있습니다");
            }

            log.info("파일 다운로드 완료: {} ({}bytes)", fileName, response.getBody().length);

            // 4. InputStreamResource로 변환하여 반환
            InputStreamResource resource =
                    new InputStreamResource(new ByteArrayInputStream(response.getBody()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(response.getBody().length)
                    .body(resource);

        } catch (Exception e) {
            log.error("파일 다운로드 실패: {} - {}", fileName, e.getMessage());
            throw new RuntimeException("파일 다운로드에 실패했습니다: " + e.getMessage(), e);
        }
    }
}