package com.example.agent_rnd.dto;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NoticeListResponse {

    private Long noticeId;
    private String title;
    private String excInsttNm;     // 집행기관명
    private String author;         // 작성기관
    private String pubDate;        // 게시일
    private String reqstDt;        // 신청기한
    private String trgetNm;        // 대상

    // ✅ 추가 정보
    private Integer fileCount;     // 첨부파일 개수
    private List<String> hashtags; // ✅ 해시태그 전체 표시

    public static NoticeListResponse from(ProjectNotice notice) {
        return new NoticeListResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getExcInsttNm(),
                notice.getAuthor(),
                notice.getPubDate(),
                notice.getReqstDt(),
                notice.getTrgetNm(),
                notice.getNoticeFiles().size(),
                notice.getHashtags().stream()
                        // .limit(3)  ← ✅ 삭제!
                        .map(h -> h.getTagName())
                        .toList()
        );
    }
}