package com.example.agent_rnd.dto;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NoticeDetailResponse {

    private Long noticeId;
    private String title;
    private String link;
    private String description;
    private String reqstDt;
    private List<AttachmentItem> attachments;

    public static NoticeDetailResponse from(ProjectNotice n) {
        return new NoticeDetailResponse(
                n.getNoticeId(),   // 🔥 수정 포인트
                n.getTitle(),
                n.getLink(),
                n.getDescription(),
                n.getReqstDt(),
                n.getAttachments().stream()
                        .map(AttachmentItem::from)
                        .toList()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class AttachmentItem {
        private Long attachmentId;
        private String originName;
        private String parseStatus;

        public static AttachmentItem from(
                com.example.agent_rnd.domain.notice.NoticeAttachment a
        ) {
            return new AttachmentItem(
                    a.getAttachmentId(),
                    a.getOriginName(),
                    a.getParseStatus().name()
            );
        }
    }
}
