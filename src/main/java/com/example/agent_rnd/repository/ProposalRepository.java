package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    void deleteByUser_UserId(Long userId);
    List<Proposal> findByUser_UserId(Long userId);

    // ✅ 추가: noticeId와 userId로 Proposal 조회
    Optional<Proposal> findByNotice_NoticeIdAndUser_UserId(Long noticeId, Long userId);
}