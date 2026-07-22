package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.entity.PaymentOrder;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import com.dealspot.repository.PaymentOrderRepository;
import com.dealspot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceCsvExportTest {

    @Mock private UserRepository userRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private PaymentOrderRepository paymentOrderRepository;

    @InjectMocks private StatsService statsService;

    private User testUser;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Ravi Kumar").phone("9876543210").build();
        testListing = Listing.builder().id(10L).title("Fresh Tomatoes").category("agricultural-products").build();
    }

    @Test
    void exportRevenueCsv_shouldReturnCorrectHeaders() {
        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());

        String csv = statsService.exportRevenueCsv(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        String headerLine = csv.split("\n")[0];
        assertEquals("ID,User,Listing,\"Amount (\u20b9)\",Status,Purpose,Created At,Paid At", headerLine);
    }

    @Test
    void exportRevenueCsv_shouldConvertPaiseToRupees() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .user(testUser)
                .listing(testListing)
                .amount(5000) // 5000 paise = ₹50.00
                .status("PAID")
                .purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 30, 0))
                .paidAt(LocalDateTime.of(2026, 1, 15, 10, 30, 5))
                .build();

        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(order));

        String csv = statsService.exportRevenueCsv(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        String[] lines = csv.split("\n");
        assertEquals(2, lines.length); // header + 1 data row
        assertTrue(lines[1].contains("50.00"));
    }

    @Test
    void exportRevenueCsv_shouldEscapeFieldsContainingCommas() {
        User userWithComma = User.builder().id(2L).name("Kumar, Ravi").phone("9000000001").build();
        PaymentOrder order = PaymentOrder.builder()
                .id(2L)
                .user(userWithComma)
                .listing(testListing)
                .amount(7500)
                .status("PAID")
                .purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 2, 1, 8, 0, 0))
                .build();

        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(order));

        String csv = statsService.exportRevenueCsv(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        String dataLine = csv.split("\n")[1];
        assertTrue(dataLine.contains("\"Kumar, Ravi\""));
    }

    @Test
    void exportRevenueCsv_shouldEscapeFieldsContainingQuotes() {
        Listing listingWithQuote = Listing.builder().id(11L).title("\"Premium\" Seeds").category("agricultural-products").build();
        PaymentOrder order = PaymentOrder.builder()
                .id(3L)
                .user(testUser)
                .listing(listingWithQuote)
                .amount(10000)
                .status("PAID")
                .purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 3, 1, 12, 0, 0))
                .build();

        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(order));

        String csv = statsService.exportRevenueCsv(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        String dataLine = csv.split("\n")[1];
        // Quotes should be escaped by doubling: "Premium" → ""Premium""
        assertTrue(dataLine.contains("\"\"\"Premium\"\" Seeds\""));
    }

    @Test
    void exportRevenueCsv_shouldHandleNullPaidAt() {
        PaymentOrder order = PaymentOrder.builder()
                .id(4L)
                .user(testUser)
                .listing(testListing)
                .amount(5000)
                .status("CREATED")
                .purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 1, 20, 9, 0, 0))
                .paidAt(null)
                .build();

        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(order));

        String csv = statsService.exportRevenueCsv(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        String dataLine = csv.split("\n")[1];
        // Last field (Paid At) should be empty
        assertTrue(dataLine.endsWith(",") || dataLine.trim().endsWith(""));
    }

    @Test
    void exportRevenueCsv_shouldIncludeAllTransactionStatuses() {
        PaymentOrder paid = PaymentOrder.builder()
                .id(5L).user(testUser).listing(testListing)
                .amount(5000).status("PAID").purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 1, 10, 10, 0, 0)).build();
        PaymentOrder failed = PaymentOrder.builder()
                .id(6L).user(testUser).listing(testListing)
                .amount(5000).status("FAILED").purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 1, 11, 10, 0, 0)).build();
        PaymentOrder created = PaymentOrder.builder()
                .id(7L).user(testUser).listing(testListing)
                .amount(5000).status("CREATED").purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 1, 12, 10, 0, 0)).build();

        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(paid, failed, created));

        String csv = statsService.exportRevenueCsv(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        String[] lines = csv.split("\n");
        assertEquals(4, lines.length); // header + 3 data rows
        assertTrue(lines[1].contains("PAID"));
        assertTrue(lines[2].contains("FAILED"));
        assertTrue(lines[3].contains("CREATED"));
    }

    @Test
    void exportRevenueCsv_shouldHandleNullUserAndListing() {
        PaymentOrder order = PaymentOrder.builder()
                .id(8L)
                .user(null)
                .listing(null)
                .amount(5000)
                .status("PAID")
                .purpose("CONTACT_UNLOCK")
                .createdAt(LocalDateTime.of(2026, 1, 5, 14, 0, 0))
                .build();

        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(order));

        String csv = statsService.exportRevenueCsv(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        String dataLine = csv.split("\n")[1];
        // Should not throw NPE; user and listing fields should be empty
        assertTrue(dataLine.startsWith("8,,"));
    }
}
