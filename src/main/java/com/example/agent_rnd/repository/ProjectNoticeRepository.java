package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectNoticeRepository extends JpaRepository<ProjectNotice, Long> {

    /**
     * 공고 상세 조회용
     * - attachments를 함께 로딩
     * - N+1 방지
     */
    @EntityGraph(attributePaths = "attachments")
    Optional<ProjectNotice> findById(Long noticeId);
}

