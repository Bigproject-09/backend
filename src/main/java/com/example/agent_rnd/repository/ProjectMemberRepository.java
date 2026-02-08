package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.project.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    // 1. 중복 초대 방지용
    boolean existsByProposal_ProposalIdAndUser_UserId(Long proposalId, Long userId);

    // 2. 특정 프로젝트의 멤버 목록 조회
    List<ProjectMember> findByProposal_ProposalId(Long proposalId);

    // ★ 3. [핵심] 내가 참여한 프로젝트 목록 조회 (MyPageService에서 쓸 예정!)
    List<ProjectMember> findByUser_UserId(Long userId);

    // 4. 내 권한 확인용 (내가 이 프로젝트의 관리자인가?)
    Optional<ProjectMember> findByProposal_ProposalIdAndUser_UserId(Long proposalId, Long userId);
}