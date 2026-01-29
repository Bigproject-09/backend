package com.example.agent_rnd.domain.notice;

import com.example.agent_rnd.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "NOTICE_ATTACHMENTS")
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
     * 어떤 공고의 첨부파일인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private ProjectNotice notice;

    /**
     * 업로드한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 화면 표시용 원본 파일명
     */
    @Column(name = "origin_name", nullable = false, length = 255)
    private String originName;

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
     * 에러 메시지 (NOT NULL)
     * 실패가 아니어도 빈 문자열 유지
     */
    @Column(name = "error_msg", nullable = false, columnDefinition = "TEXT")
    private String errorMsg;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void setNotice(ProjectNotice notice) {
        this.notice = notice;
    }


    /* =========================
       생성용 팩토리 메서드
       ========================= */

    public static NoticeAttachment create(
            ProjectNotice notice,
            User user,
            String originName
    ) {
        NoticeAttachment attachment = NoticeAttachment.builder()
                .notice(notice)
                .user(user)
                .originName(originName)
                .parseStatus(ParseStatus.WAIT)
                .errorMsg("")
                .build();

        notice.addAttachment(attachment);
        return attachment;
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
