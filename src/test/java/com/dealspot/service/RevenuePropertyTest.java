package com.dealspot.service;

import com.dealspot.repository.ListingRepository;
import com.dealspot.repository.PaymentOrderRepository;
import com.dealspot.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for revenue totals excluding non-successful payments.
 *
 * Property 6: Revenue totals exclude non-successful payments.
 * Given random sets of PaymentOrder with various statuses (PAID, FAILED, REFUNDED, CREATED),
 * the revenue total should only include PAID payments, and category breakdown sums should match total.
 *
 * Validates: Requirements REQ-REV-01, REQ-REV-02, REQ-REV-04
 */
@Tag("Feature:admin-panel")
@Tag("Property-6:Revenue-totals-exclude-non-successful-payments")
class RevenuePropertyTest {

    private static final List<String> STATUSES = List.of("PAID", "FAILED", "REFUNDED", "CREATED");
    private static final List<String> CATEGORIES = List.of("Electronics", "Agriculture", "Vehicles", "Services", "Other");

    /**
     * Property: totalRevenue equals the sum returned by repository for status "PAID" only.
     *
     * We mock the repository's sumAmountByStatusAndCreatedBetween to return a value
     * ONLY when called with "PAID". For any other status, it returns 0.
     * This verifies the service exclusively queries for PAID payments when computing revenue.
     *
     * Validates: Requirements REQ-REV-01, REQ-REV-02
     */
    @Property(tries = 100)
    void totalRevenue_onlyIncludesPaidPayments(
            @ForAll("paidRevenue") long paidAmount,
            @ForAll("dateRanges") DateRange dateRange
    ) {
        // Setup mocks
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        PaymentOrderRepository paymentOrderRepository = mock(PaymentOrderRepository.class);

        LocalDateTime fromDt = dateRange.from.atStartOfDay();
        LocalDateTime toDt = dateRange.to.atTime(LocalTime.MAX);

        // Only PAID status returns the paidAmount; any other status returns 0
        when(paymentOrderRepository.sumAmountByStatusAndCreatedBetween(eq("PAID"), any(), any()))
                .thenReturn(paidAmount);
        when(paymentOrderRepository.sumAmountByStatusAndCreatedBetween(
                argThat(s -> s != null && !s.equals("PAID")), any(), any()))
                .thenReturn(0L);

        // Daily revenue and category breakdown return empty (tested separately)
        when(paymentOrderRepository.findDailyRevenue(any(), any()))
                .thenReturn(new ArrayList<>());
        when(paymentOrderRepository.findRevenueByCategoryBetween(any(), any()))
                .thenReturn(new ArrayList<>());

        // Failed/refunded counts
        when(paymentOrderRepository.countByStatusAndCreatedAtBetween(anyString(), any(), any()))
                .thenReturn(0L);

        StatsService service = new StatsService(userRepository, listingRepository, paymentOrderRepository);

        // Act
        Map<String, Object> result = service.getRevenueStats(dateRange.from, dateRange.to);

        // Assert: totalRevenue matches paidAmount exactly
        assertEquals(paidAmount, (long) result.get("totalRevenue"),
                "totalRevenue must equal the sum of PAID payments only");

        // Verify the repository was called with "PAID" status
        verify(paymentOrderRepository).sumAmountByStatusAndCreatedBetween(eq("PAID"), eq(fromDt), eq(toDt));

        // Verify no other status was queried for revenue total
        verify(paymentOrderRepository, never()).sumAmountByStatusAndCreatedBetween(eq("FAILED"), any(), any());
        verify(paymentOrderRepository, never()).sumAmountByStatusAndCreatedBetween(eq("REFUNDED"), any(), any());
        verify(paymentOrderRepository, never()).sumAmountByStatusAndCreatedBetween(eq("CREATED"), any(), any());
    }

    /**
     * Property: Category breakdown sums match totalRevenue.
     *
     * Given random category amounts that sum to a known total, the service should
     * report totalRevenue equal to the sum of category amounts.
     *
     * Validates: Requirements REQ-REV-01, REQ-REV-04
     */
    @Property(tries = 100)
    void categoryBreakdownSums_matchTotalRevenue(
            @ForAll("categoryAmounts") List<Long> categoryAmounts,
            @ForAll("dateRanges") DateRange dateRange
    ) {
        // Setup mocks
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        PaymentOrderRepository paymentOrderRepository = mock(PaymentOrderRepository.class);

        // Compute expected total from category amounts
        long expectedTotal = categoryAmounts.stream().mapToLong(Long::longValue).sum();

        // Mock: totalRevenue returns the expectedTotal
        when(paymentOrderRepository.sumAmountByStatusAndCreatedBetween(eq("PAID"), any(), any()))
                .thenReturn(expectedTotal);

        // Mock: daily revenue empty
        when(paymentOrderRepository.findDailyRevenue(any(), any()))
                .thenReturn(new ArrayList<>());

        // Mock: category breakdown returns categories with the given amounts
        List<Object[]> categoryRaw = new ArrayList<>();
        for (int i = 0; i < categoryAmounts.size(); i++) {
            String category = CATEGORIES.get(i % CATEGORIES.size());
            categoryRaw.add(new Object[]{category, categoryAmounts.get(i), (long) (i + 1)});
        }
        when(paymentOrderRepository.findRevenueByCategoryBetween(any(), any()))
                .thenReturn(categoryRaw);

        // Mock: counts
        when(paymentOrderRepository.countByStatusAndCreatedAtBetween(anyString(), any(), any()))
                .thenReturn(0L);

        StatsService service = new StatsService(userRepository, listingRepository, paymentOrderRepository);

        // Act
        Map<String, Object> result = service.getRevenueStats(dateRange.from, dateRange.to);

        // Assert: totalRevenue equals the sum of category amounts
        long totalRevenue = (long) result.get("totalRevenue");
        assertEquals(expectedTotal, totalRevenue,
                "totalRevenue must equal sum of category breakdown amounts");

        // Assert: sum of categoryBreakdown amounts equals totalRevenue
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categoryBreakdown = (List<Map<String, Object>>) result.get("categoryBreakdown");
        long categorySum = categoryBreakdown.stream()
                .mapToLong(entry -> (long) entry.get("amount"))
                .sum();
        assertEquals(totalRevenue, categorySum,
                "Sum of category breakdown amounts must equal totalRevenue");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Long> paidRevenue() {
        return Arbitraries.longs().between(0, 10_000_000L);
    }

    @Provide
    Arbitrary<DateRange> dateRanges() {
        return Arbitraries.integers().between(1, 365)
                .flatMap(daysBack -> Arbitraries.integers().between(1, 30)
                        .map(rangeDays -> {
                            LocalDate from = LocalDate.now().minusDays(daysBack + rangeDays);
                            LocalDate to = LocalDate.now().minusDays(daysBack);
                            return new DateRange(from, to);
                        }));
    }

    @Provide
    Arbitrary<List<Long>> categoryAmounts() {
        return Arbitraries.longs().between(100L, 500_000L)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    // ─── Helper Classes ────────────────────────────────────────

    static class DateRange {
        final LocalDate from;
        final LocalDate to;

        DateRange(LocalDate from, LocalDate to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return from + " -> " + to;
        }
    }
}
