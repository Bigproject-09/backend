package com.example.agent_rnd.service;

import com.example.agent_rnd.client.BusinessVerifyClient;
import com.example.agent_rnd.dto.AuthDtos;
import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserEntityType;
import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    private final BusinessVerifyClient businessVerifyClient;
    private final EmailAuthService emailAuthService;

    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final ProposalRepository proposalRepository;
    private final PresentationRepository presentationRepository;
    private final ScriptRepository scriptRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================
    // 회사 회원가입 + ADMIN 생성 (MASTER 제거)
    // =========================
    @Transactional
    public SignupResult companySignupAndCreateAdmin(AuthDtos.CompanySignupRequest req) {

        // 🔸 DB에는 businessRegNo/openDate 저장 컬럼이 없음
        //    하지만 국세청 진위확인에는 필요할 수 있어서 "검증 용도"로만 사용
        String bno = normalizeDigits(req.businessRegNo());
        String openDtDigits = normalizeDigits(req.openDate());   // YYYYMMDD
        String ceoName = (req.ceoName() == null) ? "" : req.ceoName().trim();

        if (bno.length() != 10) throw new IllegalArgumentException("사업자등록번호는 숫자 10자리여야 합니다.");
        if (openDtDigits.length() != 8) throw new IllegalArgumentException("개업일자는 YYYYMMDD 형식이어야 합니다.");
        if (ceoName.isBlank()) throw new IllegalArgumentException("대표자명은 필수입니다.");

        String email = (req.email() == null) ? "" : req.email().trim().toLowerCase();
        if (email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");

        if (!emailAuthService.isVerified(email)) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (req.password() == null || req.password().isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다.");
        if (!req.password().equals(req.passwordConfirm())) throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");

        var verifyRes = businessVerifyClient.validate(bno, openDtDigits, ceoName);
        if (verifyRes == null || verifyRes.data() == null || verifyRes.data().isEmpty()) {
            throw new IllegalArgumentException("사업자 진위확인 응답이 비정상입니다.");
        }
        var item = verifyRes.data().get(0);
        if (!"01".equals(item.valid())) {
            throw new IllegalArgumentException("사업자 진위확인 실패: " + item.valid_msg());
        }

        String taxTypeCd = (item.status() == null) ? null : item.status().tax_type_cd();
        UserEntityType userEntityType = UserEntityType.fromTaxTypeCd(taxTypeCd);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusYears(1);

        String allowedDomain = extractDomain(email); // companies.allowed_domain (NOT NULL)

        // RegistrationPage에서 넘어온 추가 값들(JSON 컬럼에 문자열로 저장)
        String financialSummaryJson = toJsonOrNull(req.financialSummary());
        String historyJson = toJsonOrNull(req.history());
        String coreJson = toJsonOrNull(req.coreCompetency());

        // business_report_sections (JSON)
        // - 프론트에서 업로드/파싱 결과를 넘기면 여기로 저장
        // - 없으면 null 저장
        String businessReportSectionsJson = toJsonOrNull(req.businessReportSections());

        Company company = Company.create(
                req.companyName(),
                req.ceoName(),
                req.address(),
                req.industry(),
                req.employees(),
                financialSummaryJson,
                historyJson,
                coreJson,
                businessReportSectionsJson,
                now,
                end,
                userEntityType,
                allowedDomain
        );
        companyRepository.save(company);

        String encoded = passwordEncoder.encode(req.password());

        // 최상위 ADMIN (parent null)
        User admin = User.createAdmin(company, email, encoded, null);
        userRepository.save(admin);

        return new SignupResult(company.getCompanyId(), admin.getUserId());
    }

    // =========================
    // 권한별 유저 삭제
    // - MEMBER: 본인만
    // - ADMIN : 본인 + 본인 소속 MEMBER만
    // =========================
    @Transactional
    public void deleteUserByManager(Long managerUserId, Long targetUserId) {

        User manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 유저가 없습니다."));

        // 같은 회사만 가능
        if (!Objects.equals(manager.getCompany().getCompanyId(), target.getCompany().getCompanyId())) {
            throw new AccessDeniedException("다른 회사 유저는 삭제할 수 없습니다.");
        }

        // MEMBER: 자기 자신만
        if (manager.getRole() == UserRole.MEMBER) {
            if (!Objects.equals(manager.getUserId(), targetUserId)) {
                throw new AccessDeniedException("MEMBER는 본인만 삭제할 수 있습니다.");
            }
            hardDeleteUser(targetUserId);
            return;
        }

        // ADMIN
        if (manager.getRole() == UserRole.ADMIN) {

            // 본인 삭제
            if (Objects.equals(manager.getUserId(), targetUserId)) {
                hardDeleteUser(targetUserId);
                return;
            }

            // MEMBER만 삭제 가능
            if (target.getRole() != UserRole.MEMBER) {
                throw new AccessDeniedException("ADMIN은 MEMBER만 삭제할 수 있습니다.");
            }

            // target.parent == manager
            if (target.getParent() == null || !Objects.equals(target.getParent().getUserId(), manager.getUserId())) {
                throw new AccessDeniedException("ADMIN은 본인에게 속한 MEMBER만 삭제할 수 있습니다.");
            }

            hardDeleteUser(targetUserId);
            return;
        }

        throw new AccessDeniedException("삭제 권한이 없습니다.");
    }

    // =========================
    // 회사 가입 취소(회사 통째 삭제)
    // =========================
    @Transactional
    public void deleteCompanySignup(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사가 없습니다."));

        // 회사 유저 전체 삭제(하위 포함 재귀)
        userRepository.findAllByCompany_CompanyId(companyId)
                .forEach(u -> hardDeleteUser(u.getUserId()));

        companyRepository.delete(company);
    }

    // =========================
    // 실제 강제 삭제 로직(연관 데이터 정리 포함)
    // =========================
    @Transactional
    protected void hardDeleteUser(Long userId) {

        // 1) 자식 먼저 삭제 (parent_id 체인)
        userRepository.findByParent_UserId(userId)
                .forEach(child -> hardDeleteUser(child.getUserId()));

        // 2) proposal -> presentation -> script 삭제
        List<Proposal> proposals = proposalRepository.findByUser_UserId(userId);
        for (Proposal p : proposals) {
            Long proposalId = p.getProposalId();

            var presentations = presentationRepository.findByProposal_ProposalId(proposalId);
            for (var pres : presentations) {
                scriptRepository.deleteByPresentation_PresentationId(pres.getPresentationId());
            }

            presentationRepository.deleteByProposal_ProposalId(proposalId);
        }

        // 3) FK 데이터 정리
        noticeAttachmentRepository.deleteByUser_UserId(userId);
        proposalRepository.deleteByUser_UserId(userId);

        // 4) 유저 삭제
        userRepository.deleteById(userId);
    }

    private String normalizeDigits(String s) {
        return (s == null) ? "" : s.replaceAll("[^0-9]", "");
    }

    private String extractDomain(String email) {
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) return "unknown";
        return email.substring(at + 1).trim().toLowerCase();
    }

    private String toJsonOrNull(Object value) {
        try {
            if (value == null) return null;
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 변환 실패");
        }
    }

    public record SignupResult(Long companyId, Long adminUserId) {}
}
