package com.dealspot.repository;

import com.dealspot.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = :status")
    long sumAmountByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = :status AND p.createdAt >= :after")
    long sumAmountByStatusAndCreatedAfter(@Param("status") String status, @Param("after") LocalDateTime after);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = :status AND p.createdAt BETWEEN :from AND :to")
    long sumAmountByStatusAndCreatedBetween(@Param("status") String status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByStatusAndCreatedAtBetween(String status, LocalDateTime from, LocalDateTime to);

    Page<PaymentOrder> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<PaymentOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
