package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.enums.PaymentStatus;
import com.example.agent_rnd.domain.payment.Payment;
import com.example.agent_rnd.domain.plan.Plan;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.PaymentCallbackRequest;
import com.example.agent_rnd.repository.PaymentRepository;
import com.example.agent_rnd.repository.PlanRepository;
import com.example.agent_rnd.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;  // ✅ 추가
import com.siot.IamportRestClient.IamportClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.lang.reflect.Type;  // ✅ 추가
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final IamportClient iamportClient;
    private final PaymentRepository paymentRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    /**
     * 결제 검증 및 저장 메인 로직
     */
    @Transactional
    public Long processPaymentDone(PaymentCallbackRequest request) {
        log.info("📢 결제 검증 요청 시작: imp_uid={}, merchant_uid={}", request.getImp_uid(), request.getMerchant_uid());

        // 1. 프론트에서 결제 실패라고 왔으면 바로 실패 처리
        if (request.getSuccess() != null && !request.getSuccess()) {
            throw new IllegalArgumentException("결제가 실패했습니다: " + request.getError_msg());
        }

        // 2. 포트원 서버에서 진짜 결제 내역 조회 (검증)
        com.siot.IamportRestClient.response.Payment portonePayment = getPortonePayment(request.getImp_uid());

        // 3. 결제 금액 검증
        BigDecimal paidAmount = portonePayment.getAmount();

        Long userId = parseUserIdFromMerchantUid(request.getMerchant_uid());
        Integer planId = parsePlanIdFromMerchantUid(request.getMerchant_uid());

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요금제입니다."));

        if (plan.getPrice().compareTo(paidAmount) != 0) {
            throw new IllegalStateException("결제 금액 오류! (상품: " + plan.getPrice() + ", 결제: " + paidAmount + ")");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 4. DB 저장
        Payment payment = Payment.builder()
                .user(user)
                .plan(plan)
                .impUid(portonePayment.getImpUid())
                .merchantUid(portonePayment.getMerchantUid())
                .amount(paidAmount)
                .status(PaymentStatus.PAID)
                .failReason(null)
                .build();

        paymentRepository.save(payment);

        // 5. 유저 등급 UP
        user.upgradePlan(plan);
        log.info("✅ 결제 처리 완료! User ID: {}, Amount: {}", userId, paidAmount);

        return payment.getId();
    }

    private com.siot.IamportRestClient.response.Payment getPortonePayment(String impUid) {
        try {
            String accessToken = iamportClient.getAuth().getResponse().getToken();
            String url = "https://api.iamport.kr/payments/" + impUid + "?include_sandbox=true";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);
            headers.add("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("🚀 포트원 수동 조회 시도: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            Gson gson = new Gson();

            // ✅ TypeToken 사용 (경고 제거)
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> result = gson.fromJson(response.getBody(), mapType);

            Object responseData = result.get("response");

            if (responseData == null) {
                log.error("🚨 조회 결과 response가 null입니다. (진짜 없는 결제건)");
                throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다. (404)");
            }

            String jsonStr = gson.toJson(responseData);
            com.siot.IamportRestClient.response.Payment payment = gson.fromJson(jsonStr, com.siot.IamportRestClient.response.Payment.class);

            log.info("✅ 수동 조회 성공! 상태: {}", payment.getStatus());
            return payment;

        } catch (Exception e) {
            log.error("🚨 포트원 API 조회 실패: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("포트원 API 연동 에러", e);
        }
    }

    private Long parseUserIdFromMerchantUid(String uid) {
        try {
            String[] parts = uid.split("_");
            return Long.parseLong(parts[1].split("-")[1]);
        } catch (Exception e) {
            return 1L;
        }
    }

    private Integer parsePlanIdFromMerchantUid(String uid) {
        try {
            String[] parts = uid.split("_");
            return Integer.parseInt(parts[0].split("-")[1]);
        } catch (Exception e) {
            return 1;
        }
    }
}