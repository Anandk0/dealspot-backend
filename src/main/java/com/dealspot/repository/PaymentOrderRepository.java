package com.dealspot.repository;

import com.dealspot.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);
}
