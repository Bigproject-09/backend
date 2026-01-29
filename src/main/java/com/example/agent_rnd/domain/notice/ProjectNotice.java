package com.example.agent_rnd.domain.notice;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "PROJECT_NOTICES",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "seq")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProjectNotice {

    @Id
    @Column(name = "notice_id")
    private Long id; // 외부(FastAPI)에서 주입

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

    @OneToMany(mappedBy = "notice", fetch = FetchType.LAZY)
    @Builder.Default
    private List<NoticeAttachment> attachments = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addAttachment(NoticeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setNotice(this);
    }
}
