package com.dealspot.service;

import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalListings", listingRepository.count());
        stats.put("activeListings", listingRepository.countByStatus("ACTIVE"));
        stats.put("pendingModeration", listingRepository.countByStatus("PENDING"));

        // Revenue
        stats.put("totalRevenue", paymentOrderRepository.sumAmountByStatus("PAID"));
        stats.put("todayRevenue", paymentOrderRepository.sumAmountByStatusAndCreatedAfter("PAID", todayStart));
        stats.put("monthRevenue", paymentOrderRepository.sumAmountByStatusAndCreatedAfter("PAID", monthStart));
        stats.put("totalUnlocks", paymentOrderRepository.countByStatus("PAID"));

        return stats;
    }

    public Map<String, Object> getRevenueStats(LocalDate from, LocalDate to) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        stats.put("totalAmount", paymentOrderRepository.sumAmountByStatusAndCreatedBetween("PAID", fromDt, toDt));
        stats.put("totalTransactions", paymentOrderRepository.countByStatusAndCreatedAtBetween("PAID", fromDt, toDt));
        stats.put("failedTransactions", paymentOrderRepository.countByStatusAndCreatedAtBetween("FAILED", fromDt, toDt));

        return stats;
    }
}
