package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByBusinessRegNo(String businessRegNo);

    @Query("select coalesce(max(c.id), 0) from Company c")
    Long findMaxId();
}
