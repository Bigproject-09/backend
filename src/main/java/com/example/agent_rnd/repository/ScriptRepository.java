package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.script.Script;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptRepository extends JpaRepository<Script, Long> {
    void deleteByPresentation_PresentationId(Long presentationId);
}
