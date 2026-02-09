package com.example.agent_rnd.service;

import com.example.agent_rnd.client.FastApiClient;
import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CompanyBusinessReportService {

    private final CompanyRepository companyRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public void uploadAndSaveBusinessReportSections(Long companyId, MultipartFile file) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("company not found: " + companyId));

        String sectionsJson = fastApiClient.parseBusinessReportSections(file);

        // Company 엔티티의 검증 로직 사용
        company.updateBusinessReportSections(sectionsJson);

        // JPA dirty checking으로 저장됨
    }
}
