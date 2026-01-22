package com.example.agent_rnd.service;

import com.example.agent_rnd.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ProposalRepository proposalRepository;
    private final CompanyTagRepository companyTagRepository;

    @Transactional
    public void deleteCompanyCascade(Long companyId) {
        // 1) 회사 유저들 조회
        List<Long> userIds = userRepository.findIdsByCompanyId(companyId);

        // 2) 자식 테이블부터 삭제
        if (!userIds.isEmpty()) {
            proposalRepository.deleteByUserIdIn(userIds);
        }
        companyTagRepository.deleteByCompanyId(companyId);

        // 3) users -> companies 순서
        userRepository.deleteByCompanyId(companyId);
        companyRepository.deleteById(companyId);
    }
}
