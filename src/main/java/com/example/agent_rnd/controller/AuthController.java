package com.example.agent_rnd.controller;

import com.example.agent_rnd.controller.dto.AuthDtos;
import com.example.agent_rnd.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/company-signup")
    public AuthDtos.CompanySignupResponse companySignup(@RequestBody AuthDtos.CompanySignupRequest req) {
        var r = userService.companySignupAndCreateAdmin(
                req.companyName(),
                req.businessRegNo(),
                req.openDate(),
                req.ceoName(),
                req.adminEmail(),
                req.password(),
                req.passwordConfirm(),
                req.planId()
        );
        return new AuthDtos.CompanySignupResponse(r.companyId(), r.adminUserId());
    }

}
