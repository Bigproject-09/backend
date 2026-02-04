package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectNoticeRepository extends JpaRepository<ProjectNotice, Long> {

    /**
     * 공고 상세 조회용
     * - 연관된 엔티티들을 함께 로딩 (N+1 방지)
     * - noticeFiles: 공고 파일 목록
     * - hashtags: 해시태그 목록
     * - checklists: 체크리스트 목록
     * - references: 참고자료 목록
     */
    boolean existsBySeq(String seq);
    @EntityGraph(attributePaths = {
            "noticeFiles",
            "hashtags",
            "checklists",      // ✅ checklistItems → checklists
            "references"
    })
    Optional<ProjectNotice> findWithDetailsByNoticeId(Long noticeId);  // ✅ ById → ByNoticeId
}