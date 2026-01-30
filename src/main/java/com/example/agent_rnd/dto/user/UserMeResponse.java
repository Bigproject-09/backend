package com.example.agent_rnd.dto.user;

import com.example.agent_rnd.domain.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserMeResponse(
        Long userId,
        String email,
        UserRole role,

        Long companyId,
        String companyName,

        Integer planId,
        String planName,
        BigDecimal planPrice,
        boolean isDownloadable,
        String planType,  // ✅ Integer previewPage → String planType

        Long parentId,
        String parentEmail,

        LocalDateTime createdAt
) {}