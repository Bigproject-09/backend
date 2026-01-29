package com.example.agent_rnd.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentCallbackRequest {
    private String imp_uid;      // 포트원 결제 고유번호
    private String merchant_uid; // 우리 주문번호
    private Boolean success;     // 결제 성공 여부 (프론트에서 1차 판단)
    private String error_msg;    // 실패 시 에러 메시지
}