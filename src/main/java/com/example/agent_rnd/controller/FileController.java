package com.example.agent_rnd.controller;

import com.example.agent_rnd.domain.auditlog.AuditLog;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.AuditLogRepository;
import com.example.agent_rnd.repository.UserRepository;
import com.example.agent_rnd.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 파일 다운로드 (또는 URL 조회) API
    // GET /api/files/download?fileName=result.pptx
    @GetMapping("/download")
    public ResponseEntity<String> downloadFile(
            @RequestParam String fileName,
            @RequestHeader("Authorization") String authHeader
    ) {
        // 1. 유저 ID 추출
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 2. ★ [보안 로그] 다운로드 기록 저장
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action("DOWNLOAD")
                .targetDocument(fileName) // 예: "최종_제안서.pptx"
                .build());

        // 3. (임시) 실제 파일 URL 반환
        // 실제로는 S3 URL을 생성해서 리턴해주면 됩니다.
        String fileUrl = "https://my-s3-bucket.aws.com/" + fileName;

        return ResponseEntity.ok(fileUrl);
    }
}