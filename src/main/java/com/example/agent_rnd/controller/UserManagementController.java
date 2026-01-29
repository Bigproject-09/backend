package com.example.agent_rnd.controller;

import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.dto.user.CompanyUserResponse;
import com.example.agent_rnd.dto.user.UserDetailResponse;
import com.example.agent_rnd.dto.user.UserMeResponse;
import com.example.agent_rnd.repository.UserRepository;
import com.example.agent_rnd.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserService userService;
    private final UserRepository userRepository;

    // =========================
    // 1) 내 정보 조회 (토큰 필수)
    // GET /api/users/me
    // =========================
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(Authentication authentication) {
        Long myUserId = (Long) authentication.getPrincipal();

        var me = userRepository.findById(myUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        Long parentId = (me.getParent() == null) ? null : me.getParent().getUserId();
        String parentEmail = (me.getParent() == null) ? null : me.getParent().getEmail();

        return ResponseEntity.ok(new UserMeResponse(
                me.getUserId(),
                me.getEmail(),
                me.getRole(),
                me.getCompany().getCompanyId(),
                me.getCompany().getCompanyName(),
                me.getPlan().getPlanId(),
                me.getPlan().getPlanName(),
                me.getPlan().getPrice(),
                me.getPlan().isDownloadable(),
                me.getPlan().getPreviewPage(),
                parentId,
                parentEmail,
                me.getCreatedAt()
        ));
    }

    // =========================
    // 2) 회사 유저 목록 + 검색 (MASTER/ADMIN)
    // GET /api/users?role=ADMIN&email=gmail
    // =========================
    @GetMapping
    public ResponseEntity<List<CompanyUserResponse>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String email,
            Authentication authentication
    ) {
        Long myUserId = (Long) authentication.getPrincipal();

        var me = userRepository.findById(myUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        if (me.getRole() == UserRole.MEMBER) {
            throw new AccessDeniedException("조회 권한이 없습니다.");
        }

        Long companyId = me.getCompany().getCompanyId();

        var rows = userRepository.findCompanyUsersFiltered(companyId, role, email);
        var resp = rows.stream().map(v -> new CompanyUserResponse(
                v.getUserId(),
                v.getEmail(),
                v.getRole(),
                v.getCompanyId(),
                v.getCompanyName(),
                v.getParentId(),
                v.getParentEmail(),
                v.getPlanId(),
                v.getPlanName(),
                v.getPlanPrice(),
                v.getIsDownloadable(),
                v.getPreviewPage(),
                v.getCreatedAt()
        )).toList();

        return ResponseEntity.ok(resp);
    }

    // =========================
    // 3) 유저 상세 조회 (MASTER/ADMIN)
    // GET /api/users/{userId}
    // =========================
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> detail(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myUserId = (Long) authentication.getPrincipal();

        var me = userRepository.findById(myUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        if (me.getRole() == UserRole.MEMBER) {
            throw new AccessDeniedException("조회 권한이 없습니다.");
        }

        var target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("대상 유저가 없습니다."));

        if (!Objects.equals(me.getCompany().getCompanyId(), target.getCompany().getCompanyId())) {
            throw new AccessDeniedException("다른 회사 유저는 조회할 수 없습니다.");
        }

        Long parentId = (target.getParent() == null) ? null : target.getParent().getUserId();
        String parentEmail = (target.getParent() == null) ? null : target.getParent().getEmail();

        return ResponseEntity.ok(new UserDetailResponse(
                target.getUserId(),
                target.getEmail(),
                target.getRole(),
                target.getCompany().getCompanyId(),
                target.getCompany().getCompanyName(),
                target.getPlan().getPlanId(),
                target.getPlan().getPlanName(),
                parentId,
                parentEmail,
                target.getCreatedAt()
        ));
    }

    // =========================
    // 4) 특정 ADMIN 아래 멤버 목록 (MASTER/ADMIN)
    // GET /api/users/admin/{adminId}/members
    // =========================
    @GetMapping("/admin/{adminId}/members")
    public ResponseEntity<?> listMembersUnderAdmin(
            @PathVariable Long adminId,
            Authentication authentication
    ) {
        Long myUserId = (Long) authentication.getPrincipal();

        var me = userRepository.findById(myUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        if (me.getRole() == UserRole.MEMBER) {
            throw new AccessDeniedException("조회 권한이 없습니다.");
        }

        var admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("ADMIN이 없습니다."));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("adminId는 ADMIN 역할이어야 합니다.");
        }

        if (!Objects.equals(me.getCompany().getCompanyId(), admin.getCompany().getCompanyId())) {
            throw new AccessDeniedException("다른 회사의 ADMIN은 조회할 수 없습니다.");
        }

        // ADMIN이 호출하는 경우: 자기 아래만 조회 가능
        if (me.getRole() == UserRole.ADMIN && !Objects.equals(me.getUserId(), adminId)) {
            throw new AccessDeniedException("ADMIN은 자기 아래 멤버만 조회할 수 있습니다.");
        }

        // MASTER가 호출하는 경우: "자기 라인"만 허용(원치 않으면 이 블록 삭제)
        if (me.getRole() == UserRole.MASTER) {
            if (admin.getParent() == null || !Objects.equals(admin.getParent().getUserId(), me.getUserId())) {
                throw new AccessDeniedException("이 MASTER 소속 ADMIN이 아닙니다.");
            }
        }

        Long companyId = me.getCompany().getCompanyId();
        var members = userRepository.findChildrenInCompany(companyId, adminId);

        // 가볍게 Map으로 반환(DTO 더 만들기 귀찮으면 이게 제일 빠름)
        return ResponseEntity.ok(
                members.stream().map(v -> java.util.Map.of(
                        "userId", v.getUserId(),
                        "email", v.getEmail(),
                        "role", v.getRole().name(),
                        "parentId", v.getParentId(),
                        "planId", v.getPlanId(),
                        "planName", v.getPlanName(),
                        "createdAt", v.getCreatedAt()
                )).toList()
        );
    }

    // =========================
    // 5) 유저 삭제 (MASTER/ADMIN)
    // DELETE /api/users/{targetUserId}
    // =========================
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long targetUserId,
            Authentication authentication
    ) {
        Long managerUserId = (Long) authentication.getPrincipal();
        userService.deleteUserByManager(managerUserId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
