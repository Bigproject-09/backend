package com.example.agent_rnd.service;

import com.example.agent_rnd.dto.AuthDtos;
import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailAuthService emailAuthService; // 인증번호 확인용

    @Transactional
    public AuthDtos.CompanySignupResult companySignupAndCreateAdmin(AuthDtos.CompanySignupRequest req) {

        // 1. 이메일 인증번호 확인 (Redis 검증)
        if (!emailAuthService.isVerified(req.email())) {
            throw new IllegalArgumentException("이메일 인증번호가 일치하지 않거나 만료되었습니다.");
        }

        // 2. 비밀번호 확인
        if (!req.password().equals(req.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. [핵심] DB에 있는 회사 가져오기
        // 스크린샷에 1번 회사가 있으므로 1번을 가져와서 연결합니다.
        Company company = companyRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("DB에 회사 데이터(ID=1)가 없습니다."));

        // 4. 유저 생성 (MEMBER)
        // User Entity에 @Builder가 있으므로 이렇게 깔끔하게 생성 가능
        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .company(company)      // 1번 회사에 소속됨
                .role(UserRole.MEMBER) // 일반 멤버 권한
                .build();

        userRepository.save(user);

        return new AuthDtos.CompanySignupResult(company.getCompanyId(), user.getUserId());
    }

    @Transactional
    public void deleteCompanySignup(Long companyId) {
        userRepository.findAllByCompany_CompanyId(companyId)
                .forEach(u -> userRepository.delete(u));
        // 회사는 DB에 고정된 데이터이므로 삭제하지 않음 (주석 처리)
        // companyRepository.deleteById(companyId);
    }

    public void deleteUserByManager(Long managerId, Long targetId) { }
}