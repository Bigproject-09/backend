package com.example.agent_rnd.domain.notice;

import com.example.agent_rnd.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notice_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class NoticeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    /**
     * 업로드한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 연관된 파일 정보 (notice_files)
     * file_id에 UNIQUE 제약이 있어서 OneToOne 관계
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private NoticeFile noticeFile;

    /**
     * FastAPI 파싱 결과 (JSON 문자열 그대로 저장)
     */
    @Column(name = "parsed_json", columnDefinition = "JSON")
    private String parsedJson;

    /**
     * 파싱 상태
     * WAIT → PROCESSING → DONE / FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false, length = 20)
    private ParseStatus parseStatus;

    /**
     * 에러 메시지
     */
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* =========================
       생성용 팩토리 메서드
       ========================= */

    public static NoticeAttachment create(
            User user,
            NoticeFile noticeFile
    ) {
        return NoticeAttachment.builder()
                .user(user)
                .noticeFile(noticeFile)
                .parseStatus(ParseStatus.WAIT)
                .errorMsg("")
                .build();
    }

    /* =========================
       상태 변경 메서드
       ========================= */

    public void markProcessing() {
        this.parseStatus = ParseStatus.PROCESSING;
    }

    public void markDone(String parsedJson) {
        this.parsedJson = parsedJson;
        this.parseStatus = ParseStatus.DONE;
        this.errorMsg = "";
    }

    public void markFailed(String errorMsg) {
        this.parseStatus = ParseStatus.FAILED;
        this.errorMsg = errorMsg;
    }

    /* =========================
       편의 메서드
       ========================= */

    /**
     * 파일명 조회 (NoticeFile에서 가져옴)
     */
    public String getFileName() {
        return noticeFile != null ? noticeFile.getPrintFileNm() : null;
    }

    /**
     * 파일 경로 조회 (NoticeFile에서 가져옴)
     */
    public String getFilePath() {
        return noticeFile != null ? noticeFile.getPrintFlpthNm() : null;
    }

    /**
     * 공고 정보 조회 (NoticeFile을 통해 접근)
     */
    public ProjectNotice getNotice() {
        return noticeFile != null ? noticeFile.getProjectNotice() : null;
    }

    /* =========================
       동등성 비교 (PK 기준)
       ========================= */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoticeAttachment)) return false;
        NoticeAttachment that = (NoticeAttachment) o;
        return attachmentId != null && attachmentId.equals(that.attachmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(attachmentId);
    }

    /* =========================
       내부 enum
       ========================= */

    public enum ParseStatus {
        WAIT,
        PROCESSING,
        DONE,
        FAILED
    }
}