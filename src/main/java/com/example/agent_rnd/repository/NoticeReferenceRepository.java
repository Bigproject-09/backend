package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.enums.ReferenceType;
import com.example.agent_rnd.domain.notice.NoticeReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeReferenceRepository extends JpaRepository<NoticeReference, Long> {
    List<NoticeReference> findByProjectNotice_NoticeId(Long noticeId);
    List<NoticeReference> findByProjectNotice_NoticeIdAndType(Long noticeId, ReferenceType type);

    void deleteByProjectNotice_NoticeId(Long noticeId);
    void deleteByProjectNotice_NoticeIdAndType(Long noticeId, ReferenceType type);
}
