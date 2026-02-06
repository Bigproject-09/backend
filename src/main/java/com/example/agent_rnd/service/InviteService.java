//package com.example.agent_rnd.service;
//
//import com.example.agent_rnd.domain.company.Company;
//import com.example.agent_rnd.domain.enums.UserRole;
//import com.example.agent_rnd.domain.user.User;
//import com.example.agent_rnd.dto.InviteDtos;
//import com.example.agent_rnd.repository.CompanyRepository;
//import com.example.agent_rnd.repository.UserRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.security.SecureRandom;
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.Base64;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class InviteService {
//
//    private final StringRedisTemplate redis;
//    private final JavaMailSender mailSender;
//    private final UserRepository userRepository;
//    private final CompanyRepository companyRepository;
//
//    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//    private final ObjectMapper om = new ObjectMapper();
//
//    @Value("${auth.invite.ttl-seconds:86400}")
//    private long inviteTtlSeconds;
//
//    @Value("${auth.invite.base-url:http://localhost:5173}")
//    private String baseUrl;
//
//    private static final SecureRandom random = new SecureRandom();
//
//    private String inviteKey(String token) { return "auth:invite:" + token; }
//    private String usedKey(String token) { return "auth:invite:used:" + token; }
//
//    // Redis payload (plan 제거)
//    public record InvitePayload(
//            Long companyId,
//            UserRole role,     // MEMBER만
//            Long parentId,     // 초대한 ADMIN userId
//            String boundEmail
//    ) {}
//
//    @Transactional
//    public InviteDtos.SendInvitesResponse sendInvites(Long inviterUserId, List<InviteDtos.InviteItem> items) {
//
//        if (items == null || items.isEmpty()) {
//            return new InviteDtos.SendInvitesResponse(0, 0, 0, List.of(), List.of("invites가 비어있습니다."));
//        }
//
//        User inviter = userRepository.findById(inviterUserId)
//                .orElseThrow(() -> new IllegalArgumentException("초대자 유저가 없습니다."));
//
//        if (inviter.getRole() != UserRole.ADMIN) {
//            throw new IllegalArgumentException("ADMIN만 초대할 수 있습니다.");
//        }
//
//        Long companyId = inviter.getCompany().getCompanyId();
//
//        int memberSent = 0;
//        List<String> skippedAlready = new ArrayList<>();
//        List<String> errors = new ArrayList<>();
//
//        for (InviteDtos.InviteItem item : items) {
//            if (item == null) continue;
//
//            String email = normalizeEmail(item.email(), errors);
//            if (email == null) continue;
//
//            // 멤버만 초대 (MASTER/ADMIN 초대는 금지)
//            if (item.role() != UserRole.MEMBER) {
//                errors.add("현재는 MEMBER만 초대 가능합니다. email=" + email);
//                continue;
//            }
//
//            if (userRepository.existsByEmail(email)) {
//                skippedAlready.add(email);
//                continue;
//            }
//
//            // (옵션) 인원 제한이 필요하면 여기서 체크
//            // long memberCount = userRepository.countByCompany_CompanyIdAndRoleAndParent_UserId(companyId, UserRole.MEMBER, inviterUserId);
//            // if (memberCount >= 2) { errors.add("MEMBER는 ADMIN 아래 최대 2명"); continue; }
//
//            String token = generateToken();
//            InvitePayload payload = new InvitePayload(companyId, UserRole.MEMBER, inviterUserId, email);
//            saveInvite(token, payload);
//            sendInviteMail(email, token);
//
//            memberSent++;
//        }
//
//        return new InviteDtos.SendInvitesResponse(
//                0,             // adminSentCount
//                memberSent,     // memberSentCount
//                0,             // memberQueuedCount
//                skippedAlready,
//                errors
//        );
//    }
//
//    public InviteDtos.ValidateTokenResponse validateToken(String token) {
//        InvitePayload payload = loadPayload(token);
//        return new InviteDtos.ValidateTokenResponse(true, payload.role(), payload.companyId(), payload.parentId());
//    }
//
//    @Transactional
//    public InviteDtos.InviteSignupResponse signupByInvite(InviteDtos.InviteSignupRequest req) {
//
//        String token = (req.token() == null) ? "" : req.token().trim();
//        if (token.isEmpty()) throw new IllegalArgumentException("토큰이 필요합니다.");
//
//        if ("1".equals(redis.opsForValue().get(usedKey(token)))) {
//            throw new IllegalArgumentException("이미 사용된 초대 토큰입니다.");
//        }
//
//        InvitePayload payload = loadPayload(token);
//
//        String email = normalizeEmailStrict(req.email());
//        if (payload.boundEmail() != null && !payload.boundEmail().equalsIgnoreCase(email)) {
//            throw new IllegalArgumentException("초대받은 이메일로만 가입할 수 있습니다.");
//        }
//
//        if (userRepository.existsByEmail(email)) {
//            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
//        }
//
//        if (req.password() == null || req.password().isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다.");
//        if (!req.password().equals(req.passwordConfirm())) throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
//
//        if (payload.role() != UserRole.MEMBER) {
//            throw new IllegalArgumentException("현재 초대 가입은 MEMBER만 가능합니다.");
//        }
//
//        Company company = companyRepository.findById(payload.companyId())
//                .orElseThrow(() -> new IllegalArgumentException("회사 정보가 없습니다."));
//
//        User parentAdmin = userRepository.findById(payload.parentId())
//                .orElseThrow(() -> new IllegalArgumentException("상위 ADMIN이 없습니다."));
//
//        if (parentAdmin.getRole() != UserRole.ADMIN) {
//            throw new IllegalArgumentException("parent는 ADMIN이어야 합니다.");
//        }
//        if (!company.getCompanyId().equals(parentAdmin.getCompany().getCompanyId())) {
//            throw new IllegalArgumentException("회사 불일치");
//        }
//
//        String encoded = passwordEncoder.encode(req.password());
//        User created = User.createMember(company, email, encoded, parentAdmin);
//        userRepository.save(created);
//
//        redis.opsForValue().set(usedKey(token), "1", Duration.ofSeconds(inviteTtlSeconds));
//        redis.delete(inviteKey(token));
//
//        return new InviteDtos.InviteSignupResponse(
//                created.getUserId(),
//                company.getCompanyId(),
//                created.getRole(),
//                0
//        );
//    }
//
//    private void saveInvite(String token, InvitePayload payload) {
//        try {
//            String json = om.writeValueAsString(payload);
//            redis.opsForValue().set(inviteKey(token), json, Duration.ofSeconds(inviteTtlSeconds));
//        } catch (Exception e) {
//            throw new RuntimeException("초대 토큰 저장 실패", e);
//        }
//    }
//
//    private InvitePayload loadPayload(String token) {
//        String json = redis.opsForValue().get(inviteKey(token));
//        if (json == null) throw new IllegalArgumentException("토큰이 없거나 만료되었습니다.");
//        try {
//            return om.readValue(json, InvitePayload.class);
//        } catch (Exception e) {
//            throw new RuntimeException("토큰 데이터 파싱 실패", e);
//        }
//    }
//
//    private void sendInviteMail(String to, String token) {
//        String link = baseUrl + "/invite-signup?token=" + token;
//
//        SimpleMailMessage msg = new SimpleMailMessage();
//        msg.setTo(to);
//        msg.setSubject("[RanDi] MEMBER 초대 회원가입 링크");
//        msg.setText("아래 링크로 회원가입을 진행해 주세요.\n\n" + link +
//                "\n\n유효시간: " + (inviteTtlSeconds / 3600) + "시간");
//        mailSender.send(msg);
//    }
//
//    private String normalizeEmailStrict(String email) {
//        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");
//        return email.trim().toLowerCase();
//    }
//
//    private String normalizeEmail(String email, List<String> errors) {
//        try {
//            return normalizeEmailStrict(email);
//        } catch (Exception e) {
//            errors.add("이메일 형식 오류: " + email);
//            return null;
//        }
//    }
//
//    private String generateToken() {
//        byte[] buf = new byte[24];
//        random.nextBytes(buf);
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
//    }
//}
