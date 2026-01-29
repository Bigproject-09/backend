package com.example.agent_rnd.domain.enums;

public enum PaymentStatus {
    READY,      // 결제 대기
    PAID,       // 결제 완료 (검증 성공)
    CANCELLED,  // 결제 취소
    FAILED      // 결제 실패
}