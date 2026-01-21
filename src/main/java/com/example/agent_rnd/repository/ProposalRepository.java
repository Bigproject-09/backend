package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findByUserOrderByIdDesc(User user);

    @Transactional
    void deleteByUserId(Long userId);

    void deleteByUserIdIn(List<Long> userIds);
}