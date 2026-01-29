package com.example.agent_rnd.domain.notice;

import com.example.agent_rnd.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTICE_ATTACHMENTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

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
     * 실패가 아니어도 빈 문자열로 유지
     */
    @Column(name = "error_msg", nullable = false, columnDefinition = "TEXT")
    private String errorMsg;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* =========================
       생성용 팩토리 메서드
       ========================= */

    public static NoticeAttachment create(
            ProjectNotice notice,
            User user,
            String originName
    ) {
        return NoticeAttachment.builder()
                .notice(notice)
                .user(user)
                .originName(originName)
                .parseStatus(ParseStatus.WAIT)
                .errorMsg("")   // NOT NULL 보장
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

    public void setNotice(ProjectNotice notice) {
        this.notice = notice;
    }


    /* =========================
       내부 enum (현재 단계 유지)
       ========================= */

    public enum ParseStatus {
        WAIT,
        PROCESSING,
        DONE,
        FAILED
    }
}
