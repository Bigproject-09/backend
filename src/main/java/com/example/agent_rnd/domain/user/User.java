package com.example.agent_rnd.domain.user;

import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.payment.Payment;
import com.example.agent_rnd.domain.plan.Plan;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "email", nullable = false, length = 100) // DB varchar(100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private UserRole role; // 0=MASTER, 1=ADMIN, 2=MEMBER

    // self join
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // nullable
    private User parent;

    @OneToMany(mappedBy = "parent")
    private List<User> children = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Payment> payments = new ArrayList<>();

    // ===== factory =====
    private static User base(Company company, Plan plan, String email, String password, UserRole role, User parent) {
        User u = new User();
        u.company = company;
        u.plan = plan;
        u.email = email;
        u.password = password;
        u.role = role;
        u.parent = parent;
        return u;
    }

    public static User createMaster(Company company, Plan plan, String email, String password) {
        return base(company, plan, email, password, UserRole.MASTER, null);
    }

    public static User createAdmin(Company company, Plan plan, String email, String password, User masterParent) {
        return base(company, plan, email, password, UserRole.ADMIN, masterParent);
    }

    public static User createMember(Company company, Plan plan, String email, String password, User adminParent) {
        return base(company, plan, email, password, UserRole.MEMBER, adminParent);
    }

    public void upgradePlan(Plan newPlan) {
        this.plan = newPlan;
    }
}
