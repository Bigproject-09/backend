package com.example.agent_rnd.domain.plan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;

    @Column(name = "plan_name", nullable = false, length = 50)
    private String planName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_downloadable", nullable = false)
    private boolean isDownloadable;

    // ✅ preview_page 삭제하고 plan_type 추가
    @Column(name = "plan_type", length = 20)
    private String planType;  // FREE, PAID 등
}