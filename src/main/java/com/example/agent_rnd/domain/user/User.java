package com.example.agent_rnd.domain.user;

import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder; // ★ 추가
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private User parent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ★ @Builder 추가: UserService에서 유연하게 객체를 생성하기 위함
    @Builder
    public User(Company company, String email, String password, String name, UserRole role, User parent) {
        this.company = company;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.parent = parent;
    }

    // 기존 팩토리 메서드는 유지하되, 필요 시 Builder를 사용하세요.
    public static User createAdmin(Company company, String email, String encodedPassword, User parent) {
        return User.builder()
                .company(company)
                .email(email)
                .password(encodedPassword)
                .role(UserRole.ADMIN)
                .parent(parent)
                .build();
    }

    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("변경할 비밀번호가 비어있습니다.");
        }
        this.password = encodedPassword;
    }
}