package com.example.agent_rnd.controller;

import com.example.agent_rnd.dto.PaymentCallbackRequest;
import com.example.agent_rnd.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 프론트엔드 결제 완료 후 호출되는 검증 API
     */
    @PostMapping("/complete")
    public ResponseEntity<String> completePayment(@RequestBody PaymentCallbackRequest request) {
        try {
            Long paymentId = paymentService.processPaymentDone(request);
            return ResponseEntity.ok("결제 검증 및 저장 성공 (ID: " + paymentId + ")");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("결제 검증 실패: " + e.getMessage());
        }
    }
}