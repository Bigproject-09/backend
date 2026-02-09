package com.example.agent_rnd.domain.project;

import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "project_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"proposal_id", "user_id"})
)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String role; // "ADMIN" / "MEMBER"

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    public ProjectMember(Proposal proposal, User user, String role) {
        this.proposal = proposal;
        this.user = user;
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }
}
