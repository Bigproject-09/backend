package com.example.agent_rnd.dto.user;

import com.example.agent_rnd.domain.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanyUserResponse(
        Long userId,
        String email,
        UserRole role,

        Long companyId,
        String companyName,

        Long parentId,
        String parentEmail,
        LocalDateTime createdAt
) {}
