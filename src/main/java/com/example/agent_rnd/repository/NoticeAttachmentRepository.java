package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    /**
     * 공고별 사용자 업로드 첨부파일 조회
     * NoticeFile을 통해 간접 연결
     */
    @Query("SELECT na FROM NoticeAttachment na " +
            "JOIN na.noticeFile nf " +
            "WHERE nf.projectNotice.noticeId = :noticeId " +
            "ORDER BY na.createdAt ASC")
    List<NoticeAttachment> findByNoticeId(@Param("noticeId") Long noticeId);

    /**
     * 사용자별 첨부파일 조회
     */
    List<NoticeAttachment> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 파일별 첨부파일 조회 (1:1 관계이므로 Optional)
     */
    NoticeAttachment findByNoticeFile_FileId(Long fileId);

    /**
     * 사용자 삭제 시 첨부파일 정리
     */
    @Transactional
    void deleteByUser_UserId(Long userId);
}