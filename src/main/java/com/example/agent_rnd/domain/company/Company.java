package com.example.agent_rnd.domain.company;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "companies")
public class Company {

    @Id
    @Column(name = "company_id")
    private Long id;

    @Column(name = "company_name", length = 100, nullable = false)
    private String name;

    @Column(name = "business_reg_no", length = 20, nullable = false)
    private String businessRegNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", length = 20)
    private ContractStatus contractStatus;

    // [수정] DB 컬럼명인 start_date와 매핑
    @Column(name = "start_date")
    private LocalDateTime startDate;

    // [수정] DB 컬럼명인 end_date와 매핑
    @Column(name = "end_date")
    private LocalDateTime endDate;

    public Company(Long id, String name, String businessRegNo,
                   ContractStatus contractStatus,
                   LocalDateTime startDate, LocalDateTime endDate) {
        this.id = id;
        this.name = name;
        this.businessRegNo = businessRegNo;
        this.contractStatus = contractStatus;
        this.startDate = startDate;
        this.endDate = endDate;
    }

}