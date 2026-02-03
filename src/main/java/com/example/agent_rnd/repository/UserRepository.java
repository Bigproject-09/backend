package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // 특정 부모 아래 자식 목록
    List<User> findByParent_UserId(Long parentId);

    // 회사 + 역할 + parent 기준 카운트 (인원 제한용)
    long countByCompany_CompanyIdAndRoleAndParent_UserId(Long companyId, UserRole role, Long parentId);

    // 회사 안에서 특정 유저 찾기(권한 검증용)
    Optional<User> findByUserIdAndCompany_CompanyId(Long userId, Long companyId);

    // 회사 유저 전체
    List<User> findAllByCompany_CompanyId(Long companyId);
}
