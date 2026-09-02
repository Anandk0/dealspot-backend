package com.dealspot.service;

import com.dealspot.entity.Category;
import com.dealspot.entity.PaymentOrder;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Builds a map of subcategory slug → parent slug for all categories that have a parent.
     * Used to roll up subcategory data under their parent in stats/revenue breakdowns.
     */
    private Map<String, String> buildSubToParentSlugMap() {
        List<Category> all = categoryRepository.findAllByOrderBySortOrderAsc();
        Map<Long, String> idToSlug = all.stream()
                .collect(Collectors.toMap(Category::getId, Category::getSlug));

        Map<String, String> subToParent = new HashMap<>();
        for (Category cat : all) {
            if (cat.getParent() != null) {
                String parentSlug = idToSlug.get(cat.getParent().getId());
                if (parentSlug != null) {
                    subToParent.put(cat.getSlug(), parentSlug);
                }
            }
        }
        return subToParent;
    }

    /**
     * Returns KPI dashboard stats:
     * totalUsers, totalListings, activeListings, pendingModeration,
     * totalRevenue, todayRevenue, monthRevenue, totalUnlocks, todayUnlocks, conversionRate
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // User & listing counts
        stats.put("totalUsers", userRepository.count());
        stats.put("totalListings", listingRepository.count());
        stats.put("activeListings", listingRepository.countByStatus("ACTIVE"));
        stats.put("pendingModeration", listingRepository.countByStatus("PENDING"));

        // Revenue - only PAID payments
        stats.put("totalRevenue", paymentOrderRepository.sumAmountByStatus("PAID"));
        stats.put("todayRevenue", paymentOrderRepository.sumAmountByStatusAndCreatedAfter("PAID", todayStart));
        stats.put("monthRevenue", paymentOrderRepository.sumAmountByStatusAndCreatedAfter("PAID", monthStart));

        // Unlocks (PAID payments = successful contact unlocks)
        long totalUnlocks = paymentOrderRepository.countByStatus("PAID");
        long todayUnlocks = paymentOrderRepository.countPaidAfter(todayStart);
        stats.put("totalUnlocks", totalUnlocks);
        stats.put("todayUnlocks", todayUnlocks);

        // Conversion rate: unlocks / total listing views (approximate)
        // If no view data available, use total listings as denominator
        long totalListings = listingRepository.count();
        double conversionRate = totalListings > 0
                ? (double) totalUnlocks / totalListings * 100.0
                : 0.0;
        stats.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);

        return stats;
    }

    /**
     * Returns revenue stats for a date range:
     * totalRevenue, dailyRevenue (date + amount), categoryBreakdown (category + amount + count),
     * failedPayments, refundedPayments
     */
    public Map<String, Object> getRevenueStats(LocalDate from, LocalDate to) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        // Total revenue (only PAID)
        long totalRevenue = paymentOrderRepository.sumAmountByStatusAndCreatedBetween("PAID", fromDt, toDt);
        stats.put("totalRevenue", totalRevenue);

        // Daily revenue breakdown
        List<Object[]> dailyRaw = paymentOrderRepository.findDailyRevenue(fromDt, toDt);
        List<Map<String, Object>> dailyRevenue = dailyRaw.stream()
                .map(row -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date", row[0].toString());
                    entry.put("amount", ((Number) row[1]).longValue());
                    return entry;
                })
                .collect(Collectors.toList());
        stats.put("dailyRevenue", dailyRevenue);

        // Category breakdown — roll up subcategories under their parent
        List<Object[]> categoryRaw = paymentOrderRepository.findRevenueByCategoryBetween(fromDt, toDt);
        Map<String, String> subToParent = buildSubToParentSlugMap();

        Map<String, long[]> rolledUp = new LinkedHashMap<>();
        for (Object[] row : categoryRaw) {
            String slug = row[0] != null ? row[0].toString() : "unknown";
            long amount = ((Number) row[1]).longValue();
            long count = ((Number) row[2]).longValue();
            // Roll up to parent if this is a subcategory
            String key = subToParent.getOrDefault(slug, slug);
            rolledUp.computeIfAbsent(key, k -> new long[]{0L, 0L});
            rolledUp.get(key)[0] += amount;
            rolledUp.get(key)[1] += count;
        }

        List<Map<String, Object>> categoryBreakdown = rolledUp.entrySet().stream()
                .map(e -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("category", e.getKey());
                    entry.put("amount", e.getValue()[0]);
                    entry.put("count", e.getValue()[1]);
                    return entry;
                })
                .collect(Collectors.toList());
        stats.put("categoryBreakdown", categoryBreakdown);

        // Failed & refunded payment counts
        stats.put("failedPayments", paymentOrderRepository.countByStatusAndCreatedAtBetween("FAILED", fromDt, toDt));
        stats.put("refundedPayments", paymentOrderRepository.countByStatusAndCreatedAtBetween("REFUNDED", fromDt, toDt));

        return stats;
    }

    /**
     * Returns paginated transaction history with optional date range filtering.
     * If from/to are null, returns all transactions.
     */
    public Page<PaymentOrder> getTransactionHistory(int page, int size, LocalDate from, LocalDate to) {
        PageRequest pageRequest = PageRequest.of(page, size);

        if (from != null && to != null) {
            LocalDateTime fromDt = from.atStartOfDay();
            LocalDateTime toDt = to.atTime(LocalTime.MAX);
            return paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromDt, toDt, pageRequest);
        }

        return paymentOrderRepository.findAllByOrderByCreatedAtDesc(pageRequest);
    }

    /**
     * Returns user growth stats: daily registration counts for the last 30 days,
     * total registered users, and new users count for the period.
     * REQ-ANA-01: Total registered users with daily growth
     * REQ-ANA-07: User activity (new registrations per day/week)
     */
    public Map<String, Object> getUserGrowthStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // Total registered users
        stats.put("totalUsers", userRepository.count());

        // New registrations in last 30 days
        stats.put("newUsersLast30Days", userRepository.countByCreatedAtBetween(thirtyDaysAgo, now));

        // Daily registration breakdown for last 30 days
        List<Object[]> dailyRaw = userRepository.findDailyRegistrations(thirtyDaysAgo, now);
        List<Map<String, Object>> dailyRegistrations = dailyRaw.stream()
                .map(row -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date", row[0].toString());
                    entry.put("count", ((Number) row[1]).longValue());
                    return entry;
                })
                .collect(Collectors.toList());
        stats.put("dailyRegistrations", dailyRegistrations);

        return stats;
    }

    /**
     * Returns listing analytics: distribution by category and status breakdown.
     * REQ-ANA-02: Total active listings by category
     * REQ-ANA-04: Total contact unlocks (conversion metric)
     * REQ-ANA-06: Category-wise listing distribution
     */
    public Map<String, Object> getListingStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total listings
        stats.put("totalListings", listingRepository.count());

        // Category distribution — roll up subcategories under their parent
        List<Object[]> categoryRaw = listingRepository.countGroupByCategory();
        Map<String, String> subToParent = buildSubToParentSlugMap();

        Map<String, Long> rolledUp = new LinkedHashMap<>();
        for (Object[] row : categoryRaw) {
            String slug = row[0] != null ? row[0].toString() : "unknown";
            long count = ((Number) row[1]).longValue();
            String key = subToParent.getOrDefault(slug, slug);
            rolledUp.merge(key, count, Long::sum);
        }

        List<Map<String, Object>> categoryDistribution = rolledUp.entrySet().stream()
                .map(e -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("category", e.getKey());
                    entry.put("count", e.getValue());
                    return entry;
                })
                .collect(Collectors.toList());
        stats.put("categoryDistribution", categoryDistribution);

        // Status breakdown
        List<Object[]> statusRaw = listingRepository.countGroupByStatus();
        List<Map<String, Object>> statusBreakdown = statusRaw.stream()
                .map(row -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("status", row[0] != null ? row[0].toString() : "unknown");
                    entry.put("count", ((Number) row[1]).longValue());
                    return entry;
                })
                .collect(Collectors.toList());
        stats.put("statusBreakdown", statusBreakdown);

        // Contact unlock stats (conversion metric)
        long totalUnlocks = paymentOrderRepository.countByStatus("PAID");
        long totalListings = listingRepository.count();
        stats.put("totalUnlocks", totalUnlocks);
        double conversionRate = totalListings > 0
                ? (double) totalUnlocks / totalListings * 100.0
                : 0.0;
        stats.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);

        return stats;
    }

    /**
     * Exports revenue data as CSV for the given date range.
     * Includes ALL transactions (not just PAID) within the range.
     * CSV headers: ID, User, Listing, Amount (₹), Status, Purpose, Created At, Paid At
     */
    public String exportRevenueCsv(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        List<PaymentOrder> transactions = paymentOrderRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(fromDt, toDt);

        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("ID,User,Listing,\"Amount (\u20b9)\",Status,Purpose,Created At,Paid At\n");

        for (PaymentOrder order : transactions) {
            csv.append(order.getId()).append(',');
            csv.append(escapeCsvField(order.getUser() != null ? order.getUser().getName() : "")).append(',');
            csv.append(escapeCsvField(order.getListing() != null ? order.getListing().getTitle() : "")).append(',');
            csv.append(String.format("%.2f", order.getAmount() / 100.0)).append(',');
            csv.append(escapeCsvField(order.getStatus())).append(',');
            csv.append(escapeCsvField(order.getPurpose())).append(',');
            csv.append(escapeCsvField(order.getCreatedAt() != null ? order.getCreatedAt().format(dtFormatter) : "")).append(',');
            csv.append(escapeCsvField(order.getPaidAt() != null ? order.getPaidAt().format(dtFormatter) : ""));
            csv.append('\n');
        }

        return csv.toString();
    }

    /**
     * Escapes a CSV field by quoting it if it contains commas, double quotes, or newlines.
     * Double quotes within the field are escaped by doubling them.
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
