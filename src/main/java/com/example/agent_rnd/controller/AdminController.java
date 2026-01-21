package com.example.agent_rnd.controller;

import com.example.agent_rnd.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @DeleteMapping("/companies/{companyId}")
    public void deleteCompany(@PathVariable Long companyId) {
        adminService.deleteCompanyCascade(companyId);
    }
}
