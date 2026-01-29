package com.example.agent_rnd.dto.user;

import com.example.agent_rnd.domain.enums.UserRole;

import java.time.LocalDateTime;

public record UserDetailResponse(
        Long userId,
        String email,
        UserRole role,

        Long companyId,
        String companyName,

        Integer planId,
        String planName,

        Long parentId,
        String parentEmail,

        LocalDateTime createdAt
) {}
