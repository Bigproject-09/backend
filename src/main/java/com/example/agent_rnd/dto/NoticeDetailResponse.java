package com.example.agent_rnd.dto;

import com.example.agent_rnd.domain.notice.NoticeAttachment;
import com.example.agent_rnd.domain.notice.ProjectNotice;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NoticeDetailResponse {

    private Long id;
    private String title;
    private String link;
    private String description;
    private String reqstDt;

    /**
     * 사용자가 업로드한 첨부파일 목록 (NOTICE_ATTACHMENTS)
     */
    private List<AttachmentItem> attachments;

    public static NoticeDetailResponse from(ProjectNotice n) {
        return new NoticeDetailResponse(
                n.getId(),
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

        public static AttachmentItem from(NoticeAttachment a) {
            return new AttachmentItem(
                    a.getId(),
                    a.getOriginName(),
                    a.getParseStatus().name()
            );
        }
    }
}
