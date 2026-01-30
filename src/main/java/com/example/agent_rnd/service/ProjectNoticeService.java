package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.dto.NoticeDetailResponse;
import com.example.agent_rnd.dto.NoticeListResponse;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectNoticeService {

    private final ProjectNoticeRepository projectNoticeRepository;

    /**
     * 공고 목록 조회 (페이징)
     */
    public Page<NoticeListResponse> getNoticeList(Pageable pageable) {
        return projectNoticeRepository.findAll(pageable)
                .map(NoticeListResponse::from);
    }

    /**
     * 공고 상세 조회
     * - 공고 기본 정보
     * - 파일 목록 (notice_files)
     * - 해시태그 목록 (notice_hashtags)
     * - 체크리스트 (checklists)
     * - 참고자료 (notice_references)
     */
    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        ProjectNotice notice = projectNoticeRepository
                .findWithDetailsByNoticeId(noticeId)  // ✅ 변경
                .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다: " + noticeId));

        return NoticeDetailResponse.from(notice);
    }

    /**
     * 공고 존재 여부 확인
     */
    public boolean existsById(Long noticeId) {
        return projectNoticeRepository.existsById(noticeId);
    }

    /**
     * 공고 조회 (엔티티 반환)
     */
    public ProjectNotice getNotice(Long noticeId) {
        return projectNoticeRepository
                .findById(noticeId)  // 이건 그대로 (단순 조회용)
                .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다: " + noticeId));
    }
}