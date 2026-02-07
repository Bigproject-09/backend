package com.example.agent_rnd.domain.auditlog;

import com.example.agent_rnd.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★ [수정 1] User는 필수! (DB: NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String action;

    // ★ [수정 2] DB 컬럼명("target_document")과 매핑 명시 (에러 방지용)
    // 로그인은 문서가 없을 수 있으므로 nullable은 기본값(true) 유지
    @Column(name = "target_document")
    private String targetDocument;

    // ★ [수정 3] 시간은 필수! (DB: NOT NULL)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Builder
    public AuditLog(User user, String action, String targetDocument) {
        this.user = user;
        this.action = action;
        this.targetDocument = targetDocument;
    }
}