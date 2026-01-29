package com.example.agent_rnd.dto;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeListResponse {

    private Long noticeId;
    private String title;
    private String excInsttNm;
    private String pubDate;

    public static NoticeListResponse from(ProjectNotice n) {
        return new NoticeListResponse(
                n.getNoticeId(),   // 🔥 수정 포인트
                n.getTitle(),
                n.getExcInsttNm(),
                n.getPubDate()
        );
    }
}
