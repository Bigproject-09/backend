package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.plan.Plan;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.InviteDtos;
import com.example.agent_rnd.repository.CompanyRepository;
import com.example.agent_rnd.repository.PlanRepository;
import com.example.agent_rnd.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${auth.invite.ttl-seconds:86400}")
    private long inviteTtlSeconds;

    @Value("${auth.invite.base-url:http://localhost:5173}")
    private String baseUrl;

    private static final SecureRandom random = new SecureRandom();

    private String inviteKey(String token) { return "auth:invite:" + token; }
    private String usedKey(String token) { return "auth:invite:used:" + token; }

    // "이번 요청에서 초대한 ADMIN이 아직 가입 전"인 경우, 그 ADMIN 토큰에 MEMBER를 매달아 대기시킴
    private String pendingMembersKeyByAdminToken(String adminToken) {
        return "auth:invite:pending-members:" + adminToken;
    }

    // Redis payload
    public record InvitePayload(
            Long companyId,
            Integer planId,
            UserRole role,
            Long parentId,      // ADMIN이면 MASTER userId, MEMBER면 ADMIN userId
            String boundEmail
    ) {}

    public record PendingMemberEmail(String email) {}

    @Transactional
    public InviteDtos.SendInvitesResponse sendInvites(Long inviterUserId, List<InviteDtos.InviteItem> items) {

        if (items == null || items.isEmpty()) {
            return new InviteDtos.SendInvitesResponse(0, 0, 0, List.of(), List.of("invites가 비어있습니다."));
        }

        User inviter = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new IllegalArgumentException("초대자 유저가 없습니다."));

        // 회사 MASTER 찾기(플랜/부모 검증용)
        User master = userRepository.findFirstByCompany_CompanyIdAndRole(inviter.getCompany().getCompanyId(), UserRole.MASTER)
                .orElseThrow(() -> new IllegalStateException("회사 MASTER가 없습니다."));

        // 기본 검증
        if (inviter.getRole() == UserRole.MEMBER) {
            throw new IllegalArgumentException("MEMBER는 초대 권한이 없습니다.");
        }

        Long companyId = inviter.getCompany().getCompanyId();
        Integer planId = master.getPlan().getPlanId(); // 회사 플랜(=MASTER 플랜) 공유

        // 결과 집계
        int adminSent = 0;
        int memberSent = 0;
        int memberQueued = 0;
        List<String> skippedAlready = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // 1) MASTER가 "ADMIN + MEMBER(대기)"를 한 번에 보내는 경우를 위해
        //    먼저 ADMIN 토큰을 생성하고 adminKey -> token 맵을 만든다.
        Map<String, String> adminKeyToToken = new HashMap<>();
        Set<String> seenAdminKeys = new HashSet<>();

        if (inviter.getRole() == UserRole.MASTER) {
            for (InviteDtos.InviteItem item : items) {
                if (item == null) continue;

                UserRole role = item.role();
                if (role != UserRole.ADMIN) continue;

                String email = safeNormalize(item.email(), errors);
                if (email == null) continue;

                String adminKey = (item.adminKey() == null) ? "" : item.adminKey().trim();
                if (adminKey.isEmpty()) {
                    errors.add("ADMIN 초대는 adminKey가 필요합니다. email=" + email);
                    continue;
                }
                if (seenAdminKeys.contains(adminKey)) {
                    errors.add("adminKey 중복입니다. adminKey=" + adminKey);
                    continue;
                }
                seenAdminKeys.add(adminKey);

                if (userRepository.existsByEmail(email)) {
                    skippedAlready.add(email);
                    continue;
                }

                // 인원 제한: MASTER 아래 ADMIN 최대 3
                long adminCount = userRepository.countByCompany_CompanyIdAndRoleAndParent_UserId(
                        companyId, UserRole.ADMIN, inviter.getUserId()
                );
                if (adminCount + adminKeyToToken.size() >= 3) {
                    errors.add("ADMIN은 MASTER 아래 최대 3명까지 가능합니다. email=" + email);
                    continue;
                }

                // ADMIN 토큰 생성 + 저장 + 메일 발송
                String token = generateToken();
                InvitePayload payload = new InvitePayload(companyId, planId, UserRole.ADMIN, inviter.getUserId(), email);
                saveInvite(token, payload);
                sendInviteMail(email, token, UserRole.ADMIN);

                adminKeyToToken.put(adminKey, token);
                adminSent++;
            }
        }

        // 2) 이제 MEMBER 처리
        for (InviteDtos.InviteItem item : items) {
            if (item == null) continue;

            UserRole targetRole = item.role();
            if (targetRole == null) {
                errors.add("role이 비어있는 항목이 있습니다.");
                continue;
            }
            if (targetRole == UserRole.MASTER) {
                errors.add("MASTER는 초대로 생성할 수 없습니다.");
                continue;
            }

            String email = safeNormalize(item.email(), errors);
            if (email == null) continue;

            if (userRepository.existsByEmail(email)) {
                skippedAlready.add(email);
                continue;
            }

            // ADMIN은 위에서 MASTER일 때만 처리했음. (ADMIN이 ADMIN 초대는 불가)
            if (targetRole == UserRole.ADMIN) {
                if (inviter.getRole() != UserRole.MASTER) {
                    errors.add("ADMIN 초대는 MASTER만 가능합니다. email=" + email);
                }
                continue;
            }

            // 여기부터 MEMBER
            if (inviter.getRole() == UserRole.ADMIN) {
                // ADMIN은 자기 아래 MEMBER만 즉시 발송
                long memberCount = userRepository.countByCompany_CompanyIdAndRoleAndParent_UserId(
                        companyId, UserRole.MEMBER, inviter.getUserId()
                );
                if (memberCount >= 2) {
                    errors.add("MEMBER는 ADMIN 아래 최대 2명까지 가능합니다. email=" + email);
                    continue;
                }

                String token = generateToken();
                InvitePayload payload = new InvitePayload(companyId, planId, UserRole.MEMBER, inviter.getUserId(), email);
                saveInvite(token, payload);
                sendInviteMail(email, token, UserRole.MEMBER);
                memberSent++;
                continue;
            }

            // MASTER가 MEMBER 초대: parentAdminKey 필수
            String parentAdminKey = (item.parentAdminKey() == null) ? "" : item.parentAdminKey().trim();
            if (parentAdminKey.isEmpty()) {
                errors.add("MASTER가 MEMBER 초대 시 parentAdminKey가 필요합니다. email=" + email);
                continue;
            }

            // (A) parentAdminKey가 "이번 요청에서 만든 ADMIN"이면: 대기열 저장만 (메일 X)
            if (adminKeyToToken.containsKey(parentAdminKey)) {
                String adminToken = adminKeyToToken.get(parentAdminKey);
                enqueuePendingMember(adminToken, email);
                memberQueued++;
                continue;
            }

            // (B) parentAdminKey가 "이미 존재하는 ADMIN의 userId"인 경우도 지원(숫자면 id로 해석)
            Long parentAdminId = tryParseLong(parentAdminKey);
            if (parentAdminId == null) {
                errors.add("parentAdminKey를 해석할 수 없습니다(이번 요청의 adminKey도 아니고 숫자 userId도 아님). parentAdminKey=" + parentAdminKey + ", email=" + email);
                continue;
            }

            User parentAdmin = userRepository.findById(parentAdminId)
                    .orElse(null);
            if (parentAdmin == null) {
                errors.add("지정한 ADMIN이 없습니다. parentAdminId=" + parentAdminId + ", email=" + email);
                continue;
            }
            if (!Objects.equals(parentAdmin.getCompany().getCompanyId(), companyId)) {
                errors.add("다른 회사의 ADMIN에는 소속될 수 없습니다. email=" + email);
                continue;
            }
            if (parentAdmin.getRole() != UserRole.ADMIN) {
                errors.add("parentAdminId는 ADMIN 역할이어야 합니다. email=" + email);
                continue;
            }
            // 해당 ADMIN의 parent가 MASTER인지 확인(같은 라인)
            if (parentAdmin.getParent() == null || parentAdmin.getParent().getRole() != UserRole.MASTER) {
                errors.add("지정한 ADMIN의 상위가 비정상입니다. email=" + email);
                continue;
            }
            if (!Objects.equals(parentAdmin.getParent().getUserId(), inviter.getUserId())) {
                errors.add("해당 ADMIN은 이 MASTER 소속이 아닙니다. email=" + email);
                continue;
            }

            long memberCount = userRepository.countByCompany_CompanyIdAndRoleAndParent_UserId(
                    companyId, UserRole.MEMBER, parentAdmin.getUserId()
            );
            if (memberCount >= 2) {
                errors.add("MEMBER는 ADMIN 아래 최대 2명까지 가능합니다. email=" + email);
                continue;
            }

            // 즉시 발송
            String token = generateToken();
            InvitePayload payload = new InvitePayload(companyId, planId, UserRole.MEMBER, parentAdmin.getUserId(), email);
            saveInvite(token, payload);
            sendInviteMail(email, token, UserRole.MEMBER);
            memberSent++;
        }

        return new InviteDtos.SendInvitesResponse(
                adminSent,
                memberSent,
                memberQueued,
                skippedAlready,
                errors
        );
    }

    private void enqueuePendingMember(String adminToken, String memberEmail) {
        try {
            String key = pendingMembersKeyByAdminToken(adminToken);
            List<PendingMemberEmail> list = loadPendingMembers(adminToken);
            list.add(new PendingMemberEmail(memberEmail));
            redis.opsForValue().set(key, om.writeValueAsString(list), Duration.ofSeconds(inviteTtlSeconds));
        } catch (Exception e) {
            throw new RuntimeException("대기 MEMBER 저장 실패", e);
        }
    }

    private List<PendingMemberEmail> loadPendingMembers(String adminToken) {
        try {
            String key = pendingMembersKeyByAdminToken(adminToken);
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) return new ArrayList<>();
            return om.readValue(json, new TypeReference<List<PendingMemberEmail>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private int flushPendingMembersAndSend(Long companyId, Integer planId, Long createdAdminId, String adminToken) {
        List<PendingMemberEmail> pendings = loadPendingMembers(adminToken);
        if (pendings.isEmpty()) return 0;

        int sent = 0;
        for (PendingMemberEmail p : pendings) {
            String email = normalizeEmail(p.email());

            // 이미 가입됐으면 스킵
            if (userRepository.existsByEmail(email)) continue;

            // 인원 제한 재검증
            long memberCount = userRepository.countByCompany_CompanyIdAndRoleAndParent_UserId(
                    companyId, UserRole.MEMBER, createdAdminId
            );
            if (memberCount >= 2) continue;

            String token = generateToken();
            InvitePayload payload = new InvitePayload(companyId, planId, UserRole.MEMBER, createdAdminId, email);
            saveInvite(token, payload);
            sendInviteMail(email, token, UserRole.MEMBER);
            sent++;
        }

        // 대기열 삭제
        redis.delete(pendingMembersKeyByAdminToken(adminToken));
        return sent;
    }

    private void saveInvite(String token, InvitePayload payload) {
        try {
            String json = om.writeValueAsString(payload);
            redis.opsForValue().set(inviteKey(token), json, Duration.ofSeconds(inviteTtlSeconds));
        } catch (Exception e) {
            throw new RuntimeException("초대 토큰 저장 실패", e);
        }
    }

    private void sendInviteMail(String to, String token, UserRole role) {
        String link = baseUrl + "/invite-signup?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("[RanDi] " + role.name() + " 초대 회원가입 링크");
        msg.setText(
                "아래 링크로 회원가입을 진행해 주세요.\n\n" +
                        link +
                        "\n\n유효시간: " + (inviteTtlSeconds / 3600) + "시간"
        );
        mailSender.send(msg);
    }

    public InviteDtos.ValidateTokenResponse validateToken(String token) {
        InvitePayload payload = loadPayload(token);
        return new InviteDtos.ValidateTokenResponse(true, payload.role(), payload.companyId(), payload.parentId());
    }

    @Transactional
    public InviteDtos.InviteSignupResponse signupByInvite(InviteDtos.InviteSignupRequest req) {

        String token = (req.token() == null) ? "" : req.token().trim();
        if (token.isEmpty()) throw new IllegalArgumentException("토큰이 필요합니다.");

        if ("1".equals(redis.opsForValue().get(usedKey(token)))) {
            throw new IllegalArgumentException("이미 사용된 초대 토큰입니다.");
        }

        InvitePayload payload = loadPayload(token);

        String email = normalizeEmail(req.email());

        if (payload.boundEmail() != null && !payload.boundEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("초대받은 이메일로만 가입할 수 있습니다.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (req.password() == null || req.password().isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다.");
        if (!req.password().equals(req.passwordConfirm())) throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");

        Company company = companyRepository.findById(payload.companyId())
                .orElseThrow(() -> new IllegalArgumentException("회사 정보가 없습니다."));

        Plan plan = planRepository.findById(payload.planId())
                .orElseThrow(() -> new IllegalArgumentException("플랜이 존재하지 않습니다."));

        User parent = null;
        if (payload.role() != UserRole.MASTER) {
            parent = userRepository.findById(payload.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("상위(parent) 유저가 없습니다."));
        }

        // 가입 시점 재검증
        enforceHierarchyAndLimits(company.getCompanyId(), payload.role(), parent);

        String encoded = passwordEncoder.encode(req.password());

        User created;
        if (payload.role() == UserRole.ADMIN) {
            created = User.createAdmin(company, plan, email, encoded, parent); // parent=MASTER
        } else if (payload.role() == UserRole.MEMBER) {
            created = User.createMember(company, plan, email, encoded, parent); // parent=ADMIN
        } else {
            throw new IllegalArgumentException("초대로 MASTER 가입은 불가합니다.");
        }

        userRepository.save(created);

        // 토큰 사용 처리
        redis.opsForValue().set(usedKey(token), "1", Duration.ofSeconds(inviteTtlSeconds));
        redis.delete(inviteKey(token));

        // ✅ ADMIN 가입 완료 시: 대기 중 MEMBER 초대메일 자동 발송
        int spawned = 0;
        if (created.getRole() == UserRole.ADMIN) {
            spawned = flushPendingMembersAndSend(
                    company.getCompanyId(),
                    plan.getPlanId(),
                    created.getUserId(),
                    token // 이 ADMIN의 초대 토큰에 매달린 대기열
            );
        }

        return new InviteDtos.InviteSignupResponse(
                created.getUserId(),
                company.getCompanyId(),
                created.getRole(),
                spawned
        );
    }

    private void enforceHierarchyAndLimits(Long companyId, UserRole role, User parent) {
        if (role == UserRole.ADMIN) {
            if (parent == null || parent.getRole() != UserRole.MASTER) throw new IllegalArgumentException("ADMIN의 parent는 MASTER여야 합니다.");
            if (!Objects.equals(parent.getCompany().getCompanyId(), companyId)) throw new IllegalArgumentException("회사 불일치");
            long adminCount = userRepository.countByCompany_CompanyIdAndRoleAndParent_UserId(companyId, UserRole.ADMIN, parent.getUserId());
            if (adminCount >= 3) throw new IllegalArgumentException("ADMIN은 MASTER 아래 최대 3명까지 가능합니다.");
        }
        if (role == UserRole.MEMBER) {
            if (parent == null || parent.getRole() != UserRole.ADMIN) throw new IllegalArgumentException("MEMBER의 parent는 ADMIN이어야 합니다.");
            if (!Objects.equals(parent.getCompany().getCompanyId(), companyId)) throw new IllegalArgumentException("회사 불일치");
            long memberCount = userRepository.countByCompany_CompanyIdAndRoleAndParent_UserId(companyId, UserRole.MEMBER, parent.getUserId());
            if (memberCount >= 2) throw new IllegalArgumentException("MEMBER는 ADMIN 아래 최대 2명까지 가능합니다.");
        }
    }

    private InvitePayload loadPayload(String token) {
        String json = redis.opsForValue().get(inviteKey(token));
        if (json == null) throw new IllegalArgumentException("토큰이 없거나 만료되었습니다.");
        try {
            return om.readValue(json, InvitePayload.class);
        } catch (Exception e) {
            throw new RuntimeException("토큰 데이터 파싱 실패", e);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");
        return email.trim().toLowerCase();
    }

    private String safeNormalize(String email, List<String> errors) {
        try {
            return normalizeEmail(email);
        } catch (Exception e) {
            errors.add("이메일 형식 오류: " + email);
            return null;
        }
    }

    private String generateToken() {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private Long tryParseLong(String s) {
        try {
            if (s == null) return null;
            String t = s.trim();
            if (t.isEmpty()) return null;
            return Long.parseLong(t);
        } catch (Exception e) {
            return null;
        }
    }
}
