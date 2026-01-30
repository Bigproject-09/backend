package com.example.agent_rnd.domain.notice;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "project_notices",
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /**
     * 공고 파일 목록 (notice_files 테이블과 관계)
     */
    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeFile> noticeFiles = new ArrayList<>();

    /**
     * 해시태그 목록 (notice_hashtags 테이블과 관계)
     */
    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeHashtag> hashtags = new ArrayList<>();

    /**
     * 체크리스트 목록
     */
    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChecklistItem> checklists = new ArrayList<>();

    /**
     * 참고자료 목록
     */
    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeReference> references = new ArrayList<>();

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
            String trgetNm
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
                .build();
    }

    /* =========================
       연관관계 편의 메서드
       ========================= */
    public void addNoticeFile(NoticeFile noticeFile) {
        this.noticeFiles.add(noticeFile);
        noticeFile.setProjectNotice(this);
    }

    public void addHashtag(NoticeHashtag hashtag) {
        this.hashtags.add(hashtag);
        hashtag.setProjectNotice(this);
    }

    public void addChecklistItem(ChecklistItem checklist) {
        this.checklists.add(checklist);
        checklist.setProjectNotice(this);
    }

    public void addReference(NoticeReference reference) {
        this.references.add(reference);
        reference.setProjectNotice(this);
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