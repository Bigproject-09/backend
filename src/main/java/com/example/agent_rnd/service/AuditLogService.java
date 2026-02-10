package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.auditlog.AuditLog;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.AuditLogRepository;
import com.example.agent_rnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(Long userId, String action, String targetDocument) {
        if (userId == null) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        auditLogRepository.save(
                AuditLog.builder()
                        .user(user)
                        .action(action)
                        .targetDocument(targetDocument)
                        .build()
        );
    }

    @Transactional
    public void log(Long userId, String action) {
        log(userId, action, null);
    }
}
