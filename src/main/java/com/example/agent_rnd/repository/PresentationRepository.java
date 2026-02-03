package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.presentation.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresentationRepository extends JpaRepository<Presentation, Long> {
    List<Presentation> findByProposal_ProposalId(Long proposalId);
    void deleteByProposal_ProposalId(Long proposalId);
}
