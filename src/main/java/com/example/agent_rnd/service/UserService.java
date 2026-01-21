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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final DraftRepository draftRepository;
    private final ProposalRepository proposalRepository;
    private final BusinessVerifyClient businessVerifyClient;

    // 로그인용 BCrypt (SecurityConfig 없이 직접 사용)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================
    // 공통 기능
    // =========================

    // 이메일 중복 체크
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    // ID로 회원 조회
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));
    }

    // =========================
    // 로그인 (geun)
    // =========================
    public User login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "아이디 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return user;
    }

    // =========================================================
    // 회원가입 1단계: 회사 생성 + 관리자 계정 생성 (jun_front)
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

        if (bno.length() != 10)
            throw new IllegalArgumentException("사업자등록번호는 숫자 10자리여야 합니다.");
        if (startDt.length() != 8)
            throw new IllegalArgumentException("개업일자는 YYYYMMDD 형식이어야 합니다.");
        if (pnm.isBlank())
            throw new IllegalArgumentException("대표자명은 필수입니다.");

        var verifyRes = businessVerifyClient.validate(bno, startDt, pnm);
        if (verifyRes == null || verifyRes.data() == null || verifyRes.data().isEmpty()) {
            throw new IllegalArgumentException("사업자 진위확인 응답이 비정상입니다.");
        }
        var item = verifyRes.data().get(0);
        if (!"01".equals(item.valid())) {
            throw new IllegalArgumentException("사업자 진위확인 실패");
        }

        if (companyRepository.findByBusinessRegNo(bno).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (password == null || password.isBlank())
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        if (!password.equals(passwordConfirm))
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");

        int resolvedPlanId = (planId == null) ? 1 : planId;
        Plan plan = planRepository.findById(resolvedPlanId)
                .orElseThrow(() -> new IllegalArgumentException("플랜이 존재하지 않습니다."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusYears(1);

        Long nextCompanyId = companyRepository.findMaxId() + 1;
        Company company = new Company(
                nextCompanyId,
                companyName,
                bno,
                ContractStatus.PENDING,
                now,
                end
        );
        companyRepository.save(company);

        Long nextUserId = userRepository.findMaxId() + 1;

        User admin = new User(
                nextUserId,
                company,
                plan,
                adminEmail,
                sha256(password), // 회원가입 쪽은 SHA-256 유지
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                now,
                false
        );
        userRepository.save(admin);

        return new SignupResult(company.getId(), admin.getId());
    }

    // =========================================================
    // 관리자 → 사용자 여러 명 생성
    // =========================================================
    @Transactional
    public List<CreatedUser> adminCreateUsers(
            Long companyId,
            Integer planId,
            List<CreateUserParam> users
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사 정보가 없습니다."));

        int resolvedPlanId = (planId == null) ? 1 : planId;
        Plan plan = planRepository.findById(resolvedPlanId)
                .orElseThrow(() -> new IllegalArgumentException("플랜이 존재하지 않습니다."));

        LocalDateTime now = LocalDateTime.now();
        List<CreatedUser> result = new ArrayList<>();

        for (CreateUserParam u : users) {
            if (userRepository.existsByEmail(u.email())) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + u.email());
            }

            Long nextUserId = userRepository.findMaxId() + 1;
            String tempPassword = randomTempPassword();

            User user = new User(
                    nextUserId,
                    company,
                    plan,
                    u.email(),
                    sha256(tempPassword),
                    u.role(),
                    UserStatus.ACTIVE,
                    now,
                    false
            );

            userRepository.save(user);
            result.add(new CreatedUser(user.getId(), user.getEmail(), tempPassword));
        }

        return result;
    }

    // 관리자 → 사용자 삭제
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    // =========================
    // 내부 유틸
    // =========================
    private String randomTempPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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

    // =========================
    // DTO / Result record
    // =========================
    public record SignupResult(Long companyId, Long adminUserId) {}
    public record CreateUserParam(String email, UserRole role) {}
    public record CreatedUser(Long userId, String email, String tempPassword) {}
}
