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
                me.getName(),
                me.getDepartment(),
                me.getPosition(),
                me.getRole(),
                me.getCompany().getCompanyId(),
                me.getCompany().getCompanyName(),
                parentId,
                parentEmail,
                me.getCreatedAt()
        ));
    }

    // =========================
    // 1-1) 내 계정 삭제
    // DELETE /api/users/me
    // =========================
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        Long myUserId = (Long) authentication.getPrincipal();
        userService.deleteUserByManager(myUserId, myUserId);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // 2) 회사 유저 목록 + 검색 (ADMIN 전용)
    // GET /api/users?role=MEMBER&email=gmail
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

        if (me.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("조회 권한이 없습니다.");
        }

        Long companyId = me.getCompany().getCompanyId();

        var users = userRepository.findAllByCompany_CompanyId(companyId);

        String emailFilter = (email == null || email.isBlank()) ? null : email.trim().toLowerCase();

        var resp = users.stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> emailFilter == null || u.getEmail().toLowerCase().contains(emailFilter))
                .map(u -> new CompanyUserResponse(
                        u.getUserId(),
                        u.getEmail(),
                        u.getRole(),
                        u.getCompany().getCompanyId(),
                        u.getCompany().getCompanyName(),
                        (u.getParent() == null) ? null : u.getParent().getUserId(),
                        (u.getParent() == null) ? null : u.getParent().getEmail(),
                        u.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(resp);
    }

    // =========================
    // 3) 유저 상세 조회 (ADMIN 전용)
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

        if (me.getRole() != UserRole.ADMIN) {
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
                parentId,
                parentEmail,
                target.getCreatedAt()
        ));
    }

    // =========================
    // 4) 특정 ADMIN 아래 멤버 목록 (ADMIN 전용)
    // GET /api/users/admin/{adminId}/members
    // - 본인(adminId==me)만 조회 가능
    // =========================
    @GetMapping("/admin/{adminId}/members")
    public ResponseEntity<?> listMembersUnderAdmin(
            @PathVariable Long adminId,
            Authentication authentication
    ) {
        Long myUserId = (Long) authentication.getPrincipal();

        var me = userRepository.findById(myUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        if (me.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("조회 권한이 없습니다.");
        }
        if (!Objects.equals(me.getUserId(), adminId)) {
            throw new AccessDeniedException("ADMIN은 자기 아래 멤버만 조회할 수 있습니다.");
        }

        var members = userRepository.findByParent_UserId(adminId).stream()
                .filter(u -> u.getRole() == UserRole.MEMBER)
                .map(u -> java.util.Map.of(
                        "userId", u.getUserId(),
                        "email", u.getEmail(),
                        "role", u.getRole().name(),
                        "parentId", (u.getParent() == null) ? null : u.getParent().getUserId(),
                        "createdAt", u.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(members);
    }

    // =========================
    // 5) 유저 삭제
    // DELETE /api/users/{targetUserId}
    // - MEMBER: 본인만
    // - ADMIN : 본인 + 본인 소속 MEMBER
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
