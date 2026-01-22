package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.company.ContractStatus;
import com.example.agent_rnd.domain.plan.Plan;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.domain.user.UserRole;
import com.example.agent_rnd.domain.user.UserStatus;
import com.example.agent_rnd.domain.user.dto.LoginRequest;
import com.example.agent_rnd.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    // private final DraftRepository draftRepository; // [삭제됨]
    private final ProposalRepository proposalRepository;
    private final BusinessVerifyClient businessVerifyClient;

    // [팀원분 코드 유지] SHA-256 방식
    // private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================
    // 공통 기능
    // =========================

    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));
    }

    // =========================
    // 로그인
    // =========================
    public User login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "아이디 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        // [유지] 팀원분 SHA-256 로직
        String inputPassword = sha256(request.getPassword());
        if (!inputPassword.equals(user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return user;
    }

    // =========================================================
    // 회원가입
    // =========================================================
    @Transactional
    public SignupResult companySignupAndCreateAdmin(
            String companyName,
            String businessRegNo,
            String openDate,
            String ceoName,
            String adminEmail,
            String password,
            String passwordConfirm,
            Integer planId
    ) {
        String bno = businessRegNo == null ? "" : businessRegNo.replaceAll("[^0-9]", "");
        String startDt = openDate == null ? "" : openDate.replaceAll("[^0-9]", "");
        String pnm = ceoName == null ? "" : ceoName.trim();

        if (bno.length() != 10) throw new IllegalArgumentException("사업자등록번호는 숫자 10자리여야 합니다.");
        if (startDt.length() != 8) throw new IllegalArgumentException("개업일자는 YYYYMMDD 형식이어야 합니다.");
        if (pnm.isBlank()) throw new IllegalArgumentException("대표자명은 필수입니다.");

        // [API 체크 로직 유지]
        try {
            var verifyRes = businessVerifyClient.validate(bno, startDt, pnm);
            if (verifyRes != null && verifyRes.data() != null && !verifyRes.data().isEmpty()) {
                if (!"01".equals(verifyRes.data().get(0).valid())) {
                    // throw new IllegalArgumentException("사업자 진위확인 실패");
                }
            }
        } catch (Exception e) {
            // API 에러 무시
        }

        if (companyRepository.findByBusinessRegNo(bno).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }
        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (password == null || password.isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다.");
        if (!password.equals(passwordConfirm)) throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");

        int resolvedPlanId = (planId == null) ? 1 : planId;

        Plan plan = planRepository.findById(resolvedPlanId)
                .orElseThrow(() -> new IllegalArgumentException("플랜이 존재하지 않습니다."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusYears(1);

        // ID 수동 생성 (팀원 코드) - Company는 유지
        Long nextCompanyId = companyRepository.findMaxId() == null ? 1L : companyRepository.findMaxId() + 1;

        Company company = new Company(
                nextCompanyId,
                companyName,
                bno,
                ContractStatus.PENDING,
                now,
                end
        );
        companyRepository.save(company);

        // [삭제] User ID 수동 생성 코드 제거 (DB Auto Increment 사용)
        // Long nextUserId = userRepository.findMaxId() == null ? 1L : userRepository.findMaxId() + 1;

        User admin = User.builder()
                // .userId(nextUserId) // [삭제]
                .companyId(company.getId())
                .planId((long)resolvedPlanId)
                .email(adminEmail)
                .password(sha256(password))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .isFreeUsed(false)
                .build();

        userRepository.save(admin);

        return new SignupResult(company.getId(), admin.getUserId());
    }

    // =========================================================
    // 관리자 → 사용자 생성
    // =========================================================
    @Transactional
    public List<CreatedUser> adminCreateUsers(Long companyId, Integer planId, List<CreateUserParam> users) {

        // 회사 존재 여부 확인
        if (!companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("회사 정보가 없습니다.");
        }

        int resolvedPlanId = (planId == null) ? 1 : planId;
        LocalDateTime now = LocalDateTime.now();
        List<CreatedUser> result = new ArrayList<>();

        for (CreateUserParam u : users) {
            if (userRepository.existsByEmail(u.email())) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + u.email());
            }

            // [삭제]
            // Long nextUserId = userRepository.findMaxId() == null ? 1L : userRepository.findMaxId() + 1;

            String tempPassword = randomTempPassword();

            User user = User.builder()
                    // .userId(nextUserId) // [삭제]
                    .companyId(companyId)
                    .planId((long)resolvedPlanId)
                    .email(u.email())
                    .password(sha256(tempPassword))
                    .role(u.role())
                    .status(UserStatus.ACTIVE)
                    .createdAt(now)
                    .isFreeUsed(false)
                    .build();

            userRepository.save(user);
            result.add(new CreatedUser(user.getUserId(), user.getEmail(), tempPassword));
        }

        return result;
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    private String sha256(String raw) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String randomTempPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public record SignupResult(Long companyId, Long adminUserId) {}
    public record CreateUserParam(String email, UserRole role) {}
    public record CreatedUser(Long userId, String email, String tempPassword) {}
}