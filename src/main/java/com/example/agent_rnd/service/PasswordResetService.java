package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void resetPassword(String email, String currentPassword, String newPassword, String newPasswordConfirm) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");
        if (currentPassword == null || currentPassword.isBlank()) throw new IllegalArgumentException("현재 비밀번호를 입력하세요.");
        if (!newPassword.equals(newPasswordConfirm)) throw new IllegalArgumentException("새 비밀번호 확인이 일치하지 않습니다.");

        // 정책 검증 (Signup과 동일)
        validatePasswordPolicy(newPassword);

        String normalized = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 현재 비밀번호가 올바르지 않습니다.");
        }

        // 현재 비밀번호와 동일 금지 (encoded 비교가 아니라 raw 기준으로 matches로 확인)
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
    }

    private void validatePasswordPolicy(String pw) {
        if (pw == null || pw.isBlank()) throw new IllegalArgumentException("새 비밀번호를 입력하세요.");

        // SignupPage와 동일: 금지 문자
        if (pw.matches(".*[()<>\"';].*")) {
            throw new IllegalArgumentException("사용할 수 없는 특수문자가 포함되어 있습니다.");
        }

        boolean hasEng = pw.matches(".*[A-Za-z].*");
        boolean hasNum = pw.matches(".*[0-9].*");
        boolean hasSpec = pw.matches(".*[~!@#$%^&*_+\\-=\\[\\]{}|:\\\\,.?/].*");

        int typeCount = 0;
        if (hasEng) typeCount++;
        if (hasNum) typeCount++;
        if (hasSpec) typeCount++;

        int len = pw.length();

        if (typeCount >= 3) {
            if (len < 8 || len > 16) throw new IllegalArgumentException("영문/숫자/특수문자 3종 조합은 8~16자리여야 합니다.");
        } else if (typeCount >= 2) {
            if (len < 10 || len > 16) throw new IllegalArgumentException("2종 조합은 10~16자리여야 합니다.");
        } else {
            throw new IllegalArgumentException("영문, 숫자, 특수문자 중 2종류 이상 조합해야 합니다.");
        }
    }

}