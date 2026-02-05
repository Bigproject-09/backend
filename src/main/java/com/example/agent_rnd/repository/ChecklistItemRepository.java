package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByProjectNotice_NoticeId(Long noticeId);

    void deleteByProjectNotice_NoticeId(Long noticeId);

    long countByProjectNotice_NoticeId(Long noticeId);
}
