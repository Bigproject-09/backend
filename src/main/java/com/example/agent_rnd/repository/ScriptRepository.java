package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.script.Script;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Long> {
    void deleteByPresentation_PresentationId(Long presentationId);

    // ✅ 추가: presentation으로 스크립트 조회
    List<Script> findByPresentation_PresentationId(Long presentationId);
}