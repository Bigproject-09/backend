//package com.example.agent_rnd.controller;
//
//import com.example.agent_rnd.dto.InviteDtos;
//import com.example.agent_rnd.service.InviteService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/invites")
//public class InviteController {
//
//    private final InviteService inviteService;
//
//    @PostMapping("/send")
//    public ResponseEntity<InviteDtos.SendInvitesResponse> sendInvites(
//            @RequestBody InviteDtos.SendInvitesRequest req,
//            Authentication authentication
//    ) {
//        Long inviterUserId = (Long) authentication.getPrincipal();
//        InviteDtos.SendInvitesResponse res = inviteService.sendInvites(inviterUserId, req.invites());
//        return ResponseEntity.ok(res);
//    }
//
//    @GetMapping("/validate")
//    public ResponseEntity<InviteDtos.ValidateTokenResponse> validate(@RequestParam String token) {
//        return ResponseEntity.ok(inviteService.validateToken(token));
//    }
//
//    @PostMapping("/signup")
//    public ResponseEntity<InviteDtos.InviteSignupResponse> inviteSignup(@RequestBody InviteDtos.InviteSignupRequest req) {
//        return ResponseEntity.ok(inviteService.signupByInvite(req));
//    }
//}
