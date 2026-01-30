package com.example.agent_rnd.dto;

import com.example.agent_rnd.domain.notice.*;
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
    private String author;
    private String excInsttNm;
    private String pubDate;
    private String trgetNm;

    private List<NoticeFileItem> files;
    private List<String> hashtags;
    private List<ChecklistItemDto> checklists;
    private List<ReferenceItem> references;
    private List<UserAttachmentItem> userAttachments;

    public static NoticeDetailResponse from(ProjectNotice notice) {
        return new NoticeDetailResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getLink(),
                notice.getDescription(),
                notice.getReqstDt(),
                notice.getAuthor(),
                notice.getExcInsttNm(),
                notice.getPubDate(),
                notice.getTrgetNm(),

                // 공고 파일 목록
                notice.getNoticeFiles().stream()
                        .map(NoticeFileItem::from)
                        .toList(),

                // 해시태그 목록
                notice.getHashtags().stream()
                        .map(NoticeHashtag::getTagName)
                        .toList(),

                // 체크리스트 목록 ✅ 수정됨
                notice.getChecklists().stream()
                        .map(ChecklistItemDto::from)
                        .toList(),

                // 참고자료 목록
                notice.getReferences().stream()
                        .map(ReferenceItem::from)
                        .toList(),

                // 사용자 업로드 첨부파일
                notice.getNoticeFiles().stream()
                        .filter(nf -> nf.getNoticeAttachment() != null)
                        .map(nf -> UserAttachmentItem.from(nf.getNoticeAttachment()))
                        .toList()
        );
    }

    /* =========================
       내부 DTO 클래스들
       ========================= */

    @Getter
    @AllArgsConstructor
    public static class NoticeFileItem {
        private Long fileId;
        private String fileName;
        private String filePath;
        private Boolean hasParsing;

        public static NoticeFileItem from(NoticeFile file) {
            return new NoticeFileItem(
                    file.getFileId(),
                    file.getPrintFileNm(),
                    file.getPrintFlpthNm(),
                    file.getNoticeAttachment() != null
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ChecklistItemDto {
        private Long checklistId;
        private String type;
        private String content;

        public static ChecklistItemDto from(ChecklistItem item) {
            return new ChecklistItemDto(
                    item.getChecklistId(),
                    item.getType().name(),
                    item.getContent()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ReferenceItem {
        private Long referenceId;
        private String type;
        private String title;
        private String url;

        public static ReferenceItem from(NoticeReference ref) {
            return new ReferenceItem(
                    ref.getReferenceId(),
                    ref.getType().name(),
                    ref.getTitle(),
                    ref.getUrl()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class UserAttachmentItem {
        private Long attachmentId;
        private Long fileId;
        private String fileName;
        private String parseStatus;
        private String errorMsg;
        private String parsedJson;

        public static UserAttachmentItem from(NoticeAttachment attachment) {
            return new UserAttachmentItem(
                    attachment.getAttachmentId(),
                    attachment.getNoticeFile().getFileId(),
                    attachment.getFileName(),
                    attachment.getParseStatus().name(),
                    attachment.getErrorMsg(),
                    attachment.getParsedJson()
            );
        }
    }
}