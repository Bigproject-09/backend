//package com.example.agent_rnd.dto;
//
//import com.example.agent_rnd.domain.enums.UserRole;
//
//import java.util.List;
//
//public class InviteDtos {
//
//    /**
//     * ADMIN: adminKey 필수
//     * MEMBER: parentAdminKey 필수 (어느 ADMIN 밑으로 갈지)
//     */
//    public record InviteItem(
//            String email,
//            UserRole role,          // ADMIN or MEMBER
//            String adminKey,        // ADMIN일 때 필수 (예: "A1")
//            String parentAdminKey   // MEMBER일 때 필수 (예: "A1")
//    ) {}
//
//    public record SendInvitesRequest(
//            List<InviteItem> invites
//    ) {}
//
//    /**
//     * adminSentCount      : ADMIN 초대 메일 발송 수
//     * memberSentCount     : MEMBER 초대 메일 즉시 발송 수(부모 ADMIN이 이미 존재하는 경우 등)
//     * memberQueuedCount   : MEMBER 초대 대기 수(부모 ADMIN이 "이번 요청에서 초대된 ADMIN"이라 아직 가입 전인 경우)
//     * skippedAlready      : 이미 가입된 이메일이라 스킵한 목록
//     * errors              : 요청 중 실패한 항목(검증/제한 등)
//     */
//    public record SendInvitesResponse(
//            int adminSentCount,
//            int memberSentCount,
//            int memberQueuedCount,
//            List<String> skippedAlready,
//            List<String> errors
//    ) {}
//
//    public record InviteSignupRequest(
//            String token,
//            String email,
//            String password,
//            String passwordConfirm
//    ) {}
//
//    /**
//     * spawnedMemberInvitesSent: ADMIN이 가입할 때(초대회원가입 완료) 그 밑에 대기 중이던 MEMBER들에게
//     *                           실제 초대메일을 몇 건 보냈는지
//     */
//    public record InviteSignupResponse(
//            Long userId,
//            Long companyId,
//            UserRole role,
//            int spawnedMemberInvitesSent
//    ) {}
//
//    public record ValidateTokenResponse(
//            boolean valid,
//            UserRole role,
//            Long companyId,
//            Long parentId
//    ) {}
//}
