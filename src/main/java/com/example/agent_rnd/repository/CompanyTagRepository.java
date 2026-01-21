package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.tag.CompanyTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyTagRepository extends JpaRepository<CompanyTag, Long> {
    void deleteByCompanyId(Long companyId);
}