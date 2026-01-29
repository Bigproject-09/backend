package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findFirstByCompany_CompanyIdAndRole(Long companyId, UserRole role);

    // 특정 부모 아래 자식 목록
    List<User> findByParent_UserId(Long parentId);

    // 회사 + 역할 + parent 기준 카운트 (인원 제한용)
    long countByCompany_CompanyIdAndRoleAndParent_UserId(Long companyId, UserRole role, Long parentId);

    // 회사 안에서 특정 유저 찾기(권한 검증용)
    Optional<User> findByUserIdAndCompany_CompanyId(Long userId, Long companyId);

    // =========================
    // ✅ 회사 유저 목록(필터/검색)용 Projection
    // =========================
    interface CompanyUserView {
        Long getUserId();
        String getEmail();
        UserRole getRole();

        Long getCompanyId();
        String getCompanyName();

        Long getParentId();
        String getParentEmail();

        Integer getPlanId();
        String getPlanName();
        BigDecimal getPlanPrice();
        boolean getIsDownloadable();
        Integer getPreviewPage();

        LocalDateTime getCreatedAt();
    }

    /**
     * ✅ 회사 유저 목록 + 검색(선택)
     * - role: null이면 전체
     * - email: null이면 전체, 포함검색
     */
    @Query("""
        select
            u.userId as userId,
            u.email as email,
            u.role as role,

            u.company.companyId as companyId,
            u.company.companyName as companyName,

            u.parent.userId as parentId,
            u.parent.email as parentEmail,

            u.plan.planId as planId,
            u.plan.planName as planName,
            u.plan.price as planPrice,
            u.plan.isDownloadable as isDownloadable,
            u.plan.previewPage as previewPage,

            u.createdAt as createdAt
        from User u
        where u.company.companyId = :companyId
          and (:role is null or u.role = :role)
          and (:email is null or lower(u.email) like lower(concat('%', :email, '%')))
        order by u.userId
    """)
    List<CompanyUserView> findCompanyUsersFiltered(
            @Param("companyId") Long companyId,
            @Param("role") UserRole role,
            @Param("email") String email
    );

    // =========================
    // ✅ 특정 ADMIN 아래 자식(MEMBER) 목록 Projection
    // =========================
    interface ChildUserView {
        Long getUserId();
        String getEmail();
        UserRole getRole();
        Long getParentId();

        Integer getPlanId();
        String getPlanName();

        LocalDateTime getCreatedAt();
    }

    @Query("""
        select
            u.userId as userId,
            u.email as email,
            u.role as role,
            u.parent.userId as parentId,
            u.plan.planId as planId,
            u.plan.planName as planName,
            u.createdAt as createdAt
        from User u
        where u.company.companyId = :companyId
          and u.parent.userId = :parentId
        order by u.userId
    """)
    List<ChildUserView> findChildrenInCompany(
            @Param("companyId") Long companyId,
            @Param("parentId") Long parentId
    );
}
