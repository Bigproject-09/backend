package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.domain.enums.UserRole;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.AuthDtos;
import com.example.agent_rnd.repository.CompanyRepository;
import com.example.agent_rnd.repository.UserRepository;
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
    private final EmailAuthService emailAuthService;

    @Transactional
    public AuthDtos.CompanySignupResult companySignupAndCreateAdmin(AuthDtos.CompanySignupRequest req) {
        if (!emailAuthService.isVerified(req.email())) {
            throw new IllegalArgumentException("이메일 인증번호가 일치하지 않거나 만료되었습니다.");
        }

        if (!req.password().equals(req.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String name = requireText(req.name(), "이름");
        String department = requireText(req.department(), "부서");
        String position = requireText(req.position(), "직책");

        Company company = companyRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("DB에 회사 데이터(ID=1)가 없습니다."));

        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .name(name)
                .department(department)
                .position(position)
                .company(company)
                .role(UserRole.MEMBER)
                .build();

        userRepository.save(user);
        return new AuthDtos.CompanySignupResult(company.getCompanyId(), user.getUserId());
    }

    @Transactional
    public void deleteCompanySignup(Long companyId) {
        userRepository.findAllByCompany_CompanyId(companyId)
                .forEach(userRepository::delete);
    }

    public void deleteUserByManager(Long managerId, Long targetId) {}

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
