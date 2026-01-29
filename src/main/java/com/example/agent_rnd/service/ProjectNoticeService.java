package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.dto.NoticeDetailResponse;
import com.example.agent_rnd.dto.NoticeListResponse;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectNoticeService {

    private final ProjectNoticeRepository projectNoticeRepository;
    private final RestTemplate restTemplate;

    /**
     * 공고 목록 조회
     */
    public Page<NoticeListResponse> getNoticeList(Pageable pageable) {
        return projectNoticeRepository.findAll(pageable)
                .map(NoticeListResponse::from);
    }

    /**
     * 공고 상세 조회
     */
    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        ProjectNotice notice = projectNoticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공고 없음"));

        return NoticeDetailResponse.from(notice);
    }

    /**
     * 기업마당 첨부파일 다운로드
     * (print_flpth_nm + print_file_nm 사용)
     */
    public ResponseEntity<InputStreamResource> downloadNoticeFile(Long noticeId) {

        ProjectNotice notice = projectNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공고 없음"));

        String path = notice.getPrintFlpthNm();   // ex) /uss/file/download/
        String fileName = notice.getPrintFileNm();

        if (path == null || fileName == null) {
            throw new IllegalStateException("첨부파일 경로 정보 없음");
        }

        String downloadUrl;

        if (path.startsWith("http")) {
            // 이미 전체 URL인 경우
            downloadUrl = path;
        } else {
            // 상대경로인 경우만 도메인 붙이기
            downloadUrl = "https://www.bizinfo.go.kr" + path;
        }


        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.setAccept(MediaType.parseMediaTypes("*/*"));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("다운로드 결과가 비어있음");
            }

            InputStreamResource resource =
                    new InputStreamResource(new ByteArrayInputStream(response.getBody()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("기업마당 첨부파일 다운로드 실패", e);
        }
    }
}
