package com.example.agent_rnd.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class ScriptSaveRequest {
    private Long noticeId;
    private List<SlideDto> slides;
    private List<QnaDto> qna;

    @Getter
    @NoArgsConstructor
    public static class SlideDto {
        private Integer page;
        private String title;
        private String script;
    }

    @Getter
    @NoArgsConstructor
    public static class QnaDto {
        private String question;
        private String answer;
        private String tips;
    }
}