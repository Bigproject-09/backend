package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.project.ProjectMember;
import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.project.MemberInviteRequest;
import com.example.agent_rnd.repository.ProjectMemberRepository;
import com.example.agent_rnd.repository.ProposalRepository;
import com.example.agent_rnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProposalRepository proposalRepository;

    public void inviteMember(Long requesterId, Long proposalId, MemberInviteRequest req) {
        // 1. 프로젝트 확인
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        // 2. 요청자(나) 권한 확인 (ADMIN만 초대 가능)
        ProjectMember requester = projectMemberRepository.findByProposal_ProposalIdAndUser_UserId(proposalId, requesterId)
                .orElseThrow(() -> new IllegalArgumentException("당신은 이 프로젝트의 멤버가 아닙니다."));

        if (!"ADMIN".equals(requester.getRole())) {
            throw new SecurityException("관리자(ADMIN) 권한이 있어야 팀원을 초대할 수 있습니다.");
        }

        // 3. 대상 유저 확인
        User targetUser = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 4. 이미 초대된 멤버인지 확인
        if (projectMemberRepository.existsByProposal_ProposalIdAndUser_UserId(proposalId, targetUser.getUserId())) {
            throw new IllegalArgumentException("이미 참여 중인 멤버입니다.");
        }

        // 5. 저장
        projectMemberRepository.save(ProjectMember.builder()
                .proposal(proposal)
                .user(targetUser)
                .role(req.getRole())
                .build());
    }
}