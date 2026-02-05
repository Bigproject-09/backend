package com.example.agent_rnd.domain.notice;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
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
     * FastAPI(모델링) 결과 원본 JSON
     * - MySQL json 컬럼을 그대로 문자열로 매핑
     */
    @Column(name = "notice_parsing_json", columnDefinition = "json")
    private String noticeParsingJson;

    @Column(name = "notice_sections_json", columnDefinition = "json")
    private String noticeSectionsJson;

    @Column(name = "analysis_json", columnDefinition = "json")
    private String analysisJson;

    @Column(name = "checklist_json", columnDefinition = "json")
    private String checklistJson;

    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoticeFile> noticeFiles = new HashSet<>();

    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoticeHashtag> hashtags = new HashSet<>();

    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ChecklistItem> checklists = new HashSet<>();

    @OneToMany(mappedBy = "projectNotice", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoticeReference> references = new HashSet<>();

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

    public static ProjectNotice ofSeq(
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
