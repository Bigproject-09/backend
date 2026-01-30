package com.example.agent_rnd.domain.notice;

import com.example.agent_rnd.domain.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "notice_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class NoticeReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reference_id")
    private Long referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    @Setter
    private ProjectNotice projectNotice;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ReferenceType type;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    /* =========================
       정적 팩토리 메서드
       ========================= */

    public static NoticeReference of(
            ProjectNotice projectNotice,
            ReferenceType type,
            String title,
            String url
    ) {
        NoticeReference reference = NoticeReference.builder()
                .projectNotice(projectNotice)
                .type(type)
                .title(title)
                .url(url)
                .build();

        if (projectNotice != null) {
            projectNotice.addReference(reference);
        }

        return reference;
    }

    /* =========================
       동등성 비교 (PK 기준)
       ========================= */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoticeReference)) return false;
        NoticeReference that = (NoticeReference) o;
        return referenceId != null && referenceId.equals(that.referenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(referenceId);
    }
}