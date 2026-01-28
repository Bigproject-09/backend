package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    void deleteByUserId(Long userId);
}
