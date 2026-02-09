package com.example.agent_rnd.controller;

import com.example.agent_rnd.service.CompanyBusinessReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyBusinessReportController {

    private final CompanyBusinessReportService service;

    @PostMapping("/{companyId}/business-report")
    public ResponseEntity<?> uploadBusinessReport(
            @PathVariable Long companyId,
            @RequestPart("file") MultipartFile file
    ) {
        service.uploadAndSaveBusinessReportSections(companyId, file);
        return ResponseEntity.ok().body(java.util.Map.of("status", "success"));
    }
}
