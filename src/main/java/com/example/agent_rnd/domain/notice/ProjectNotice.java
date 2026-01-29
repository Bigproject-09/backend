package com.example.agent_rnd.domain.notice;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "PROJECT_NOTICES",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "seq")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ProjectNotice {

    @Id
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "seq", nullable = false, length = 100)
    private String seq;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "link", nullable = false, length = 1000)
    private String link;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "exc_instt_nm", nullable = false, length = 100)
    private String excInsttNm;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "pub_date", nullable = false, length = 50)
    private String pubDate;

    @Column(name = "reqst_dt", length = 100)
    private String reqstDt;

    @Column(name = "trget_nm", nullable = false, length = 200)
    private String trgetNm;

    @Column(name = "print_flpth_nm", nullable = false, length = 500)
    private String printFlpthNm;

    @Column(name = "print_file_nm", nullable = false, length = 200)
    private String printFileNm;

    @Column(name = "hash_tags", nullable = false, length = 500)
    private String hashTags;

    /**
     * 사용자 업로드 첨부파일
     */
    @OneToMany(mappedBy = "notice", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<NoticeAttachment> attachments = new ArrayList<>();

    /* =========================
       정적 팩토리 메서드
       ========================= */
    public static ProjectNotice of(
            Long noticeId,
            String seq,
            String title,
            String link,
            String author,
            String excInsttNm,
            String description,
            String pubDate,
            String reqstDt,
            String trgetNm,
            String printFlpthNm,
            String printFileNm,
            String hashTags
    ) {
        return ProjectNotice.builder()
                .noticeId(noticeId)
                .seq(seq)
                .title(title)
                .link(link)
                .author(author)
                .excInsttNm(excInsttNm)
                .description(description)
                .pubDate(pubDate)
                .reqstDt(reqstDt)
                .trgetNm(trgetNm)
                .printFlpthNm(printFlpthNm)
                .printFileNm(printFileNm)
                .hashTags(hashTags)
                .build();
    }

    public void addAttachment(NoticeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setNotice(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectNotice)) return false;
        ProjectNotice that = (ProjectNotice) o;
        return noticeId != null && noticeId.equals(that.noticeId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(noticeId);
    }
}
