package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
