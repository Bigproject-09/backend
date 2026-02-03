package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    void deleteByUser_UserId(Long userId);
    List<Proposal> findByUser_UserId(Long userId);
}
