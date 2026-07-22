package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for FIFO moderation queue ordering.
 *
 * Validates: Requirements REQ-MOD-06
 */
@Tag("admin-panel")
@Tag("moderation-queue-fifo-ordering")
class ModerationQueueOrderingPropertyTest {

    /**
     * Property 5: Moderation queue maintains FIFO ordering.
     *
     * For any set of listings with random createdAt timestamps, the moderation queue
     * must request them sorted by createdAt ascending (oldest first), ensuring FIFO order.
     *
     * Validates: Requirements REQ-MOD-06
     */
    @Property(tries = 200)
    void moderationQueue_requestsFifoOrdering(
            @ForAll("randomListings") List<Listing> listings,
            @ForAll @IntRange(min = 0, max = 10) int page,
            @ForAll @IntRange(min = 1, max = 50) int size
    ) {
        // Arrange
        ListingRepository listingRepository = mock(ListingRepository.class);
        AuditService auditService = mock(AuditService.class);
        NotificationService notificationService = mock(NotificationService.class);
        ModerationService moderationService = new ModerationService(listingRepository, auditService, notificationService);

        // Sort the listings by createdAt ascending to simulate what the DB would return
        List<Listing> sortedListings = new ArrayList<>(listings);
        sortedListings.sort(Comparator.comparing(Listing::getCreatedAt));

        // Mock the repository to return sorted listings
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(listingRepository.findByStatus(eq("PENDING"), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(sortedListings));

        // Act
        Page<Listing> result = moderationService.getModerationQueue(page, size);

        // Assert - Verify the Pageable has ascending sort by createdAt
        Pageable capturedPageable = pageableCaptor.getValue();
        Sort sort = capturedPageable.getSort();

        // Sort must not be unsorted
        assertTrue(sort.isSorted(), "Moderation queue must specify a sort order");

        // Sort must be by createdAt
        Sort.Order createdAtOrder = sort.getOrderFor("createdAt");
        assertNotNull(createdAtOrder, "Moderation queue must sort by createdAt");

        // Sort direction must be ascending (FIFO: oldest first)
        assertEquals(Sort.Direction.ASC, createdAtOrder.getDirection(),
                "Moderation queue must sort createdAt in ascending order (FIFO)");

        // Verify the result is in ascending createdAt order
        List<Listing> resultList = result.getContent();
        for (int i = 1; i < resultList.size(); i++) {
            LocalDateTime prev = resultList.get(i - 1).getCreatedAt();
            LocalDateTime curr = resultList.get(i).getCreatedAt();
            assertTrue(prev.compareTo(curr) <= 0,
                    "Queue must return listings in FIFO order (ascending createdAt). " +
                    "Found " + prev + " before " + curr);
        }
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<List<Listing>> randomListings() {
        Arbitrary<Listing> listingArbitrary = Arbitraries.longs().between(1, 10000)
                .flatMap(id -> Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30)
                        .flatMap(title -> randomDateTime()
                                .map(createdAt -> Listing.builder()
                                        .id(id)
                                        .title(title)
                                        .category("agricultural-products")
                                        .status("PENDING")
                                        .createdAt(createdAt)
                                        .build())));

        return listingArbitrary.list().ofMinSize(1).ofMaxSize(20);
    }

    private Arbitrary<LocalDateTime> randomDateTime() {
        // Generate random timestamps within a reasonable range
        return Arbitraries.longs()
                .between(
                        LocalDateTime.of(2023, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC),
                        LocalDateTime.of(2025, 12, 31, 23, 59).toEpochSecond(java.time.ZoneOffset.UTC)
                )
                .map(epoch -> LocalDateTime.ofEpochSecond(epoch, 0, java.time.ZoneOffset.UTC));
    }
}
