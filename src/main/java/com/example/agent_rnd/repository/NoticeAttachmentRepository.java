package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    /**
     * 특정 공고에 속한 사용자 업로드 첨부파일 목록 조회
     * (업로드 시간 기준 오름차순)
     */
    List<NoticeAttachment> findByNotice_IdOrderByCreatedAtAsc(Long noticeId);
    void deleteByUser_UserId(Long userId);
}
