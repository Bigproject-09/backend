package com.example.agent_rnd.domain.company;

import com.example.agent_rnd.domain.enums.ContractStatus;
import com.example.agent_rnd.domain.enums.UserEntityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    // DB 컬럼명이 ceoName (언더스코어 아님)
    @Column(name = "ceoName", nullable = true, length = 50)
    private String ceoName;

    @Column(name = "address", nullable = true, length = 255)
    private String address;

    @Column(name = "industry", nullable = true, length = 100)
    private String industry;

    @Column(name = "employees", nullable = true)
    private Long employees;

    // json 컬럼들 (MySQL JSON) - String으로 저장/조회
    @Column(name = "financial_summary", columnDefinition = "json", nullable = true)
    private String financialSummary;

    @Column(name = "history", columnDefinition = "json", nullable = true)
    private String history;

    @Column(name = "core_competency", columnDefinition = "json", nullable = true)
    private String coreCompetency;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", nullable = false, length = 20)
    private ContractStatus contractStatus;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_entity_type", nullable = false, length = 20)
    private UserEntityType userEntityType;

    @Column(name = "allowed_domain", nullable = false, length = 50)
    private String allowedDomain;

    private Company(
            String companyName,
            String ceoName,
            String address,
            String industry,
            Long employees,
            String financialSummary,
            String history,
            String coreCompetency,
            ContractStatus contractStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UserEntityType userEntityType,
            String allowedDomain
    ) {
        this.companyName = companyName;
        this.ceoName = ceoName;
        this.address = address;
        this.industry = industry;
        this.employees = employees;
        this.financialSummary = financialSummary;
        this.history = history;
        this.coreCompetency = coreCompetency;
        this.contractStatus = contractStatus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userEntityType = userEntityType;
        this.allowedDomain = allowedDomain;
    }

    public static Company create(
            String companyName,
            String ceoName,
            String address,
            String industry,
            Long employees,
            String financialSummary,
            String history,
            String coreCompetency,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UserEntityType userEntityType,
            String allowedDomain
    ) {
        if (companyName == null || companyName.isBlank()) throw new IllegalArgumentException("companyName is required");
        if (allowedDomain == null || allowedDomain.isBlank()) throw new IllegalArgumentException("allowedDomain is required");

        return new Company(
                companyName.trim(),
                (ceoName == null || ceoName.isBlank()) ? null : ceoName.trim(),
                (address == null || address.isBlank()) ? null : address.trim(),
                (industry == null || industry.isBlank()) ? null : industry.trim(),
                employees,
                financialSummary,
                history,
                coreCompetency,
                ContractStatus.PENDING, // DB 기본값과 맞춤(현재 덤프에서 PENDING 사용)
                startDate,
                endDate,
                userEntityType,
                allowedDomain.trim().toLowerCase()
        );
    }
}
