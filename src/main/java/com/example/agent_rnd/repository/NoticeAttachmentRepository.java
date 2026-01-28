package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {
    void deleteByUser_UserId(Long userId);
}
