package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.company.Company;
import com.example.agent_rnd.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyReportService {

    private final CompanyRepository companyRepository;
    private final NoticeAttachmentParseService parseService;

    public Map<String, Object> uploadBusinessReport(Long companyId, MultipartFile file) throws Exception {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사 없음 companyId=" + companyId));

        // FastAPI /parse mode=sections 호출 → sections JSON(배열)을 문자열로 받음
        String sectionsJson = parseService.parseWithMode(file, "sections");

        // Company JSON 컬럼 업데이트
        company.updateBusinessReportSections(sectionsJson);

        // JPA dirty checking으로도 되지만, 명시적으로 save 해도 OK
        companyRepository.save(company);

        return Map.of(
                "updated", true,
                "companyId", companyId
        );
    }
}
