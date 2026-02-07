package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.auditlog.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // 필요한 경우 추가 쿼리 메서드 작성 가능
    // 예: 특정 유저의 로그만 보기 -> List<AuditLog> findByUser_Id(Long userId);

    // 기본적으로 findAll(Sort sort)를 사용할 것이므로 비워둬도 됩니다.
}