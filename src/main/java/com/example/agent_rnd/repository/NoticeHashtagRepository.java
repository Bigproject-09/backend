package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.NoticeHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeHashtagRepository extends JpaRepository<NoticeHashtag, Long> {
    List<NoticeHashtag> findByProjectNotice_NoticeId(Long noticeId);
}
