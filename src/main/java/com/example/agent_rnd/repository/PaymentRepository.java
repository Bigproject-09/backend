package com.example.agent_rnd.repository;

import com.example.agent_rnd.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 주문번호로 찾기
    Optional<Payment> findByMerchantUid(String merchantUid);

    // 포트원 번호로 찾기
    Optional<Payment> findByImpUid(String impUid);
    // 삭제 기능 추가
    void deleteByUser_UserId(Long userId);
}