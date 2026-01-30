package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.NoticeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeFileRepository extends JpaRepository<NoticeFile, Long> {

    /**
     * 특정 공고의 파일 목록 조회
     */
    List<NoticeFile> findByProjectNotice_NoticeId(Long noticeId);
}