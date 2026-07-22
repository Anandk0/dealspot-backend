package com.dealspot.service;

import com.dealspot.entity.PaymentOrder;
import com.dealspot.repository.ListingRepository;
import com.dealspot.repository.PaymentOrderRepository;
import com.dealspot.repository.UserRepository;
import net.jqwik.api.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for transaction date filter.
 *
 * Validates: Requirements REQ-REV-03
 */
@Tag("admin-panel")
@Tag("transaction-date-filter")
class TransactionDateFilterPropertyTest {

    /**
     * Property 10: Transaction filter results respect filter criteria.
     *
     * For any set of random transactions with random createdAt timestamps, applying a random
     * date range filter [from, to] must return only those transactions where createdAt falls
     * within [from.atStartOfDay(), to.atTime(23:59:59.999999999)] inclusive.
     *
     * Validates: Requirements REQ-REV-03
     */
    @Property(tries = 200)
    void transactionFilter_returnsOnlyTransactionsWithinDateRange(
            @ForAll("randomPaymentOrders") List<PaymentOrder> allOrders,
            @ForAll("randomDateRange") DateRange dateRange
    ) {
        // Arrange
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        PaymentOrderRepository paymentOrderRepository = mock(PaymentOrderRepository.class);
        StatsService statsService = new StatsService(userRepository, listingRepository, paymentOrderRepository);

        LocalDate from = dateRange.from;
        LocalDate to = dateRange.to;
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        // Filter orders that fall within the date range (simulating what the DB would return)
        List<PaymentOrder> filteredOrders = allOrders.stream()
                .filter(order -> {
                    LocalDateTime createdAt = order.getCreatedAt();
                    return !createdAt.isBefore(fromDt) && !createdAt.isAfter(toDt);
                })
                .collect(Collectors.toList());

        // Mock repository to return the filtered results
        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                eq(fromDt), eq(toDt), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(filteredOrders));

        // Act
        Page<PaymentOrder> result = statsService.getTransactionHistory(0, 50, from, to);

        // Assert - All returned transactions must have createdAt within [fromDt, toDt]
        List<PaymentOrder> resultList = result.getContent();

        for (PaymentOrder order : resultList) {
            LocalDateTime createdAt = order.getCreatedAt();
            assertNotNull(createdAt, "createdAt must not be null");
            assertFalse(createdAt.isBefore(fromDt),
                    "Transaction createdAt " + createdAt + " is before filter start " + fromDt);
            assertFalse(createdAt.isAfter(toDt),
                    "Transaction createdAt " + createdAt + " is after filter end " + toDt);
        }

        // Assert - result count matches expected filtered count
        assertEquals(filteredOrders.size(), resultList.size(),
                "Service must return exactly the transactions within the date range");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<List<PaymentOrder>> randomPaymentOrders() {
        Arbitrary<PaymentOrder> orderArbitrary = Arbitraries.longs().between(1, 10000)
                .flatMap(id -> Arbitraries.integers().between(100, 100000)
                        .flatMap(amount -> randomDateTime()
                                .map(createdAt -> PaymentOrder.builder()
                                        .id(id)
                                        .razorpayOrderId("order_" + id)
                                        .amount(amount)
                                        .status("PAID")
                                        .createdAt(createdAt)
                                        .build())));

        return orderArbitrary.list().ofMinSize(1).ofMaxSize(30);
    }

    @Provide
    Arbitrary<DateRange> randomDateRange() {
        return Arbitraries.longs()
                .between(
                        LocalDate.of(2023, 1, 1).toEpochDay(),
                        LocalDate.of(2025, 12, 31).toEpochDay()
                )
                .flatMap(startEpoch -> Arbitraries.longs()
                        .between(startEpoch, startEpoch + 365)
                        .map(endEpoch -> new DateRange(
                                LocalDate.ofEpochDay(startEpoch),
                                LocalDate.ofEpochDay(endEpoch)
                        )));
    }

    private Arbitrary<LocalDateTime> randomDateTime() {
        return Arbitraries.longs()
                .between(
                        LocalDateTime.of(2022, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 12, 31, 23, 59).toEpochSecond(ZoneOffset.UTC)
                )
                .map(epoch -> LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC));
    }

    // ─── Helper Record ────────────────────────────────────

    record DateRange(LocalDate from, LocalDate to) {}
}
