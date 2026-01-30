package com.example.agent_rnd.domain.notice;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "notice_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class NoticeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    /**
     * 어떤 공고의 파일인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    @Setter
    private ProjectNotice projectNotice;

    /**
     * 파일명 (예: 공고문.pdf)
     */
    @Column(name = "print_file_nm", nullable = false, length = 200)
    private String printFileNm;

    /**
     * 파일 경로 (S3 URL 등)
     */
    @Column(name = "print_flpth_nm", nullable = false, length = 500)
    private String printFlpthNm;

    /**
     * 파싱 정보 (1:1 관계)
     */
    @OneToOne(mappedBy = "noticeFile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private NoticeAttachment noticeAttachment;

    /* =========================
       정적 팩토리 메서드
       ========================= */

    public static NoticeFile of(
            ProjectNotice projectNotice,
            String printFileNm,
            String printFlpthNm
    ) {
        NoticeFile noticeFile = NoticeFile.builder()
                .projectNotice(projectNotice)
                .printFileNm(printFileNm)
                .printFlpthNm(printFlpthNm)
                .build();

        if (projectNotice != null) {
            projectNotice.addNoticeFile(noticeFile);
        }

        return noticeFile;
    }

    /* =========================
       동등성 비교 (PK 기준)
       ========================= */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoticeFile)) return false;
        NoticeFile that = (NoticeFile) o;
        return fileId != null && fileId.equals(that.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileId);
    }
}