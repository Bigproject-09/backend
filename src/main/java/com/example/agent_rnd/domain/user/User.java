package com.example.agent_rnd.domain.user;

import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    // DB: tinyint (0=ADMIN, 1=MEMBER)
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private User parent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private User(Company company, String email, String password, UserRole role, User parent) {
        this.company = company;
        this.email = email;
        this.password = password;
        this.role = role;
        this.parent = parent;
    }

    public static User createAdmin(Company company, String email, String encodedPassword, User parent) {
        return new User(company, email, encodedPassword, UserRole.ADMIN, parent);
    }

    public static User createMember(Company company, String email, String encodedPassword, User parentAdmin) {
        if (parentAdmin == null || parentAdmin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("MEMBER의 parent는 ADMIN이어야 합니다.");
        }
        return new User(company, email, encodedPassword, UserRole.MEMBER, parentAdmin);
    }
    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("변경할 비밀번호가 비어있습니다.");
        }
        this.password = encodedPassword;
    }

}
