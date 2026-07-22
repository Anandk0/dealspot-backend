package com.dealspot.repository;

import com.dealspot.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    // Daily revenue breakdown: returns [date, sum] pairs for PAID payments within date range
    @Query("SELECT CAST(p.paidAt AS LocalDate), COALESCE(SUM(p.amount), 0) " +
           "FROM PaymentOrder p " +
           "WHERE p.status = 'PAID' AND p.paidAt BETWEEN :from AND :to " +
           "GROUP BY CAST(p.paidAt AS LocalDate) " +
           "ORDER BY CAST(p.paidAt AS LocalDate) ASC")
    List<Object[]> findDailyRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Category breakdown: returns [category, sum, count] for PAID payments within date range
    @Query("SELECT p.listing.category, COALESCE(SUM(p.amount), 0), COUNT(p) " +
           "FROM PaymentOrder p " +
           "WHERE p.status = 'PAID' AND p.paidAt BETWEEN :from AND :to " +
           "GROUP BY p.listing.category")
    List<Object[]> findRevenueByCategoryBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Transaction history with date range filtering
    Page<PaymentOrder> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    // All transactions within a date range (for CSV export, no pagination)
    List<PaymentOrder> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    // Count today's unlocks (PAID payments with purpose CONTACT_UNLOCK)
    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = 'PAID' AND p.createdAt >= :after")
    long countPaidAfter(@Param("after") LocalDateTime after);
}
