package com.example.agent_rnd.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private String title;
    private String status;      // 예: "작성중", "완료"
    private LocalDateTime updatedAt;
}