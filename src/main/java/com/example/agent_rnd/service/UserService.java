package com.example.agent_rnd.service;

import com.example.agent_rnd.dto.AuthDtos;
import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.plan.Plan;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyTagRepository companyTagRepository;
    private final PlanRepository planRepository;
    private final BusinessVerifyClient businessVerifyClient;
    private final EmailAuthService emailAuthService;

    // 삭제에 필요한 repo
    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final ProposalRepository proposalRepository;
    private final PaymentRepository paymentRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 회사 최초 가입 = MASTER 생성
    @Transactional
    public SignupResult companySignupAndCreateMaster(AuthDtos.CompanySignupRequest req) {

        String bno = normalizeDigits(req.businessRegNo());
        String startDt = normalizeDigits(req.openDate());
        String pnm = (req.ceoName() == null) ? "" : req.ceoName().trim();

        if (bno.length() != 10) throw new IllegalArgumentException("사업자등록번호는 숫자 10자리여야 합니다.");
        if (startDt.length() != 8) throw new IllegalArgumentException("개업일자는 YYYYMMDD 형식이어야 합니다.");
        if (pnm.isBlank()) throw new IllegalArgumentException("대표자명은 필수입니다.");

        if (companyRepository.existsByBusinessRegNo(bno)) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }
        if (!emailAuthService.isVerified(req.email())) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (req.password() == null || req.password().isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다.");
        if (!req.password().equals(req.passwordConfirm())) throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");

        var verifyRes = businessVerifyClient.validate(bno, startDt, pnm);
        if (verifyRes == null || verifyRes.data() == null || verifyRes.data().isEmpty()) {
            throw new IllegalArgumentException("사업자 진위확인 응답이 비정상입니다.");
        }
        var item = verifyRes.data().get(0);
        if (!"01".equals(item.valid())) {
            throw new IllegalArgumentException("사업자 진위확인 실패: " + item.valid_msg());
        }

        int resolvedPlanId = (req.planId() == null) ? 1 : req.planId();
        Plan plan = planRepository.findById(resolvedPlanId)
                .orElseThrow(() -> new IllegalArgumentException("플랜이 존재하지 않습니다."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusYears(1);

        Company company = Company.create(req.companyName(), bno, now, end);
        companyRepository.save(company);

        String encoded = passwordEncoder.encode(req.password());

        // MASTER로 생성 (parent=null)
        User master = User.createMaster(company, plan, req.email().trim().toLowerCase(), encoded);
        userRepository.save(master);

        return new SignupResult(company.getCompanyId(), master.getUserId());
    }

    // 회사 삭제(테스트/관리용): 회사 전체 삭제 시 하위 데이터 정리 필요
    @Transactional
    public void deleteCompanySignup(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사가 없습니다."));

        // 회사 유저들 전부 삭제(자식 -> 부모 순서)
        // 1) MEMBER 삭제
        userRepository.findAll().stream()
                .filter(u -> Objects.equals(u.getCompany().getCompanyId(), companyId) && u.getRole() == UserRole.MEMBER)
                .forEach(u -> hardDeleteUser(u.getUserId()));

        // 2) ADMIN 삭제
        userRepository.findAll().stream()
                .filter(u -> Objects.equals(u.getCompany().getCompanyId(), companyId) && u.getRole() == UserRole.ADMIN)
                .forEach(u -> hardDeleteUser(u.getUserId()));

        // 3) MASTER 삭제
        userRepository.findAll().stream()
                .filter(u -> Objects.equals(u.getCompany().getCompanyId(), companyId) && u.getRole() == UserRole.MASTER)
                .forEach(u -> hardDeleteUser(u.getUserId()));

        // 태그 매핑 삭제
        companyTagRepository.deleteByCompany_CompanyId(companyId);

        companyRepository.delete(company);
    }

    // MASTER/ADMIN이 유저 삭제할 때 사용할 메서드(컨트롤러에서 호출하도록 확장 가능)
    @Transactional
    public void deleteUserByManager(Long managerUserId, Long targetUserId) {
        User manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new IllegalArgumentException("요청자 유저가 없습니다."));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 유저가 없습니다."));

        if (!Objects.equals(manager.getCompany().getCompanyId(), target.getCompany().getCompanyId())) {
            throw new IllegalArgumentException("다른 회사 유저는 삭제할 수 없습니다.");
        }
        if (target.getRole() == UserRole.MASTER) {
            throw new IllegalArgumentException("MASTER는 삭제할 수 없습니다.");
        }

        if (manager.getRole() == UserRole.MASTER) {
            // MASTER는 같은 회사면 가능. 단, ADMIN 삭제 시 해당 ADMIN이 자기 라인인지도 체크 권장
            if (target.getRole() == UserRole.ADMIN) {
                if (target.getParent() == null || !Objects.equals(target.getParent().getUserId(), manager.getUserId())) {
                    throw new IllegalArgumentException("이 MASTER 소속 ADMIN이 아닙니다.");
                }
            }
            if (target.getRole() == UserRole.MEMBER) {
                // MEMBER의 parent ADMIN이 이 MASTER 라인인지 확인
                if (target.getParent() == null || target.getParent().getRole() != UserRole.ADMIN) {
                    throw new IllegalArgumentException("MEMBER의 parent가 비정상입니다.");
                }
                User parentAdmin = target.getParent();
                if (parentAdmin.getParent() == null || !Objects.equals(parentAdmin.getParent().getUserId(), manager.getUserId())) {
                    throw new IllegalArgumentException("이 MASTER 라인의 MEMBER가 아닙니다.");
                }
            }
        } else if (manager.getRole() == UserRole.ADMIN) {
            // ADMIN은 자기 아래 MEMBER만
            if (target.getRole() != UserRole.MEMBER) throw new IllegalArgumentException("ADMIN은 MEMBER만 삭제할 수 있습니다.");
            if (target.getParent() == null || !Objects.equals(target.getParent().getUserId(), manager.getUserId())) {
                throw new IllegalArgumentException("내 소속 MEMBER만 삭제할 수 있습니다.");
            }
        } else {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // ADMIN 삭제면 하위 MEMBER 먼저 삭제
        if (target.getRole() == UserRole.ADMIN) {
            userRepository.findByParent_UserId(target.getUserId())
                    .forEach(child -> hardDeleteUser(child.getUserId()));
        }

        hardDeleteUser(target.getUserId());
    }

    // FK 걸린 것들 먼저 삭제 후 USERS 삭제
    @Transactional
    protected void hardDeleteUser(Long userId) {
        // 자식 먼저(안전)
        userRepository.findByParent_UserId(userId)
                .forEach(child -> hardDeleteUser(child.getUserId()));

        // FK 데이터 정리
        noticeAttachmentRepository.deleteByUser_UserId(userId);
        proposalRepository.deleteByUserId(userId);
        paymentRepository.deleteByUser_UserId(userId);

        userRepository.deleteById(userId);
    }

    private String normalizeDigits(String s) {
        return (s == null) ? "" : s.replaceAll("[^0-9]", "");
    }

    public record SignupResult(Long companyId, Long adminUserId) {}
}
