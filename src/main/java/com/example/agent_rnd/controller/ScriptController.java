package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.ScriptSaveRequest;
import com.example.agent_rnd.security.JwtTokenProvider;
import com.example.agent_rnd.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/save")
    public ResponseEntity<Void> saveScript(
            @RequestBody ScriptSaveRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // JWT에서 userId 추출
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getUserId(token);  // ✅ getUserIdFromToken → getUserId

        scriptService.saveScript(request, userId);
        return ResponseEntity.ok().build();
    }
}
