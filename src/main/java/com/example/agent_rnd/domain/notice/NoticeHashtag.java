package com.example.agent_rnd.domain.notice;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "notice_hashtags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class NoticeHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id")
    private Long hashtagId;

    /**
     * 어떤 공고의 해시태그인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    @Setter
    private ProjectNotice projectNotice;

    /**
     * 태그 이름
     */
    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;

    /* =========================
       정적 팩토리 메서드
       ========================= */

    public static NoticeHashtag of(
            ProjectNotice projectNotice,
            String tagName
    ) {
        NoticeHashtag hashtag = NoticeHashtag.builder()
                .projectNotice(projectNotice)
                .tagName(tagName)
                .build();

        if (projectNotice != null) {
            projectNotice.addHashtag(hashtag);
        }

        return hashtag;
    }

    /* =========================
       동등성 비교 (PK 기준)
       ========================= */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoticeHashtag)) return false;
        NoticeHashtag that = (NoticeHashtag) o;
        return hashtagId != null && hashtagId.equals(that.hashtagId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hashtagId);
    }
}
