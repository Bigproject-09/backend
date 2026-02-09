package com.example.agent_rnd.controller;

import com.example.agent_rnd.repository.UserRepository;
import com.example.agent_rnd.service.CompanyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyReportController {

    private final CompanyReportService companyReportService;
    private final UserRepository userRepository;

    @PostMapping(value = "/me/business-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyCompanyReport(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        Long userId = (Long) authentication.getPrincipal();
        var me = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Long companyId = me.getCompany().getCompanyId();
        return ResponseEntity.ok(companyReportService.uploadBusinessReport(companyId, file));
    }
}
