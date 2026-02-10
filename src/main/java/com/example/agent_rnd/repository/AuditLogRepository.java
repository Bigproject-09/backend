package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.auditlog.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // 내 로그(멤버/관리자 공통으로 본인 것만)
    Page<AuditLog> findByUser_UserId(Long userId, Pageable pageable);

    /**
     * 관리자용 전체 로그 검색(필터 옵션)
     * - userId: 특정 유저만
     * - action: 특정 액션만
     * - keyword: targetDocument 또는 user email에 포함 검색
     */
    @Query("""
        select l from AuditLog l
        join l.user u
        where (:userId is null or u.userId = :userId)
          and (:action is null or l.action = :action)
          and (
                :keyword is null
                or lower(l.targetDocument) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%'))
              )
    """)
    Page<AuditLog> searchLogs(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
