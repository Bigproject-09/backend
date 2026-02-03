package com.example.agent_rnd.dto;

public class PasswordResetDtos {
    public record ResetPasswordRequest(
            String email,
            String currentPassword,
            String newPassword,
            String newPasswordConfirm
    ) {}

    public record ResetPasswordResponse(String message) {}
}
