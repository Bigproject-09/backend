package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.draft.Draft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DraftRepository extends JpaRepository<Draft, Long> {
    void deleteByUserId(Long userId);

    void deleteByUserIdIn(List<Long> userIds);
}
