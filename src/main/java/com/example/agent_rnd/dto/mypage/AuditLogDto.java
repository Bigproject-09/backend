package com.example.agent_rnd.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String userName;       // 누가
    private String action;         // 어떤 행동을 (LOGIN, DOWNLOAD, GENERATE)
    private String targetDocument; // 무엇을 (파일명 등)
    private LocalDateTime timestamp; // 언제
}