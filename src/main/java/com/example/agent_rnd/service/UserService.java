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

    @Transactional
    public AuthDtos.CompanySignupResult companySignupAndCreateAdmin(AuthDtos.CompanySignupRequest req) {
        // 1. 회사 생성
        Company company = Company.create(
                req.companyName(),
                req.ceoName(),
                req.address(),
                req.industry(),
                req.employees(),
                null, null, null,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusYears(1),
                com.example.agent_rnd.domain.enums.UserEntityType.PROFIT,
                "example.com"
        );
        companyRepository.save(company);

        // 2. 유저 생성
        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .company(company)
                .role(UserRole.MEMBER)
                .build();

        userRepository.save(user);

        return new AuthDtos.CompanySignupResult(company.getCompanyId(), user.getUserId());
    }

    @Transactional
    public void deleteCompanySignup(Long companyId) {
        userRepository.findAllByCompany_CompanyId(companyId)
                .forEach(u -> userRepository.delete(u));
        companyRepository.deleteById(companyId);
    }

    public void deleteUserByManager(Long managerId, Long targetId) { }
}