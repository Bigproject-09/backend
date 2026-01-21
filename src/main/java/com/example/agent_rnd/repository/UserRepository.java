package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일로 회원 찾기 (로그인 시 필요)
    Optional<User> findByEmail(String email);

    // 이메일 중복 검사
    boolean existsByEmail(String email);

    @Query("select coalesce(max(u.id), 0) from User u")
    Long findMaxId();

    @Query("select u.id from User u where u.company.id = :companyId")
    List<Long> findIdsByCompanyId(@Param("companyId") Long companyId);

    void deleteByCompanyId(Long companyId);
}