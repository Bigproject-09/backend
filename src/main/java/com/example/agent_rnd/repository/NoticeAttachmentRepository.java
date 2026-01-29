package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    // 공고별 첨부파일 조회
    List<NoticeAttachment> findByNotice_NoticeIdOrderByCreatedAtAsc(Long noticeId);

    // 사용자 삭제 시 첨부파일 정리
    @Transactional
    void deleteByUser_UserId(Long userId);
}
