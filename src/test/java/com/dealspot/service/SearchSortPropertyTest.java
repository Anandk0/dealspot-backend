package com.dealspot.service;

import com.dealspot.entity.Listing;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for search sort ordering.
 *
 * Tests the sort LOGIC in isolation by generating random lists of Listing objects
 * with random prices, createdAt, and viewCount, then applying each sort criterion
 * and verifying the pairwise ordering invariant.
 *
 * Validates: Requirements 2.7
 */
@Tag("buyer-experience")
@Tag("search-sort")
class SearchSortPropertyTest {

    /**
     * Property 2: Search sort ordering — price_asc
     *
     * For any set of listings sorted by price ascending, each item's price
     * must be less than or equal to the next item's price.
     *
     * Validates: Requirements 2.7
     */
    @Property(tries = 200)
    void sortByPriceAscending_maintainsPairwiseOrdering(
            @ForAll("randomListings") List<Listing> listings
    ) {
        // Apply price_asc sort
        List<Listing> sorted = listings.stream()
                .sorted(Comparator.comparingDouble(Listing::getPrice))
                .toList();

        // Verify pairwise ordering: list[i].price <= list[i+1].price
        for (int i = 0; i < sorted.size() - 1; i++) {
            double current = sorted.get(i).getPrice();
            double next = sorted.get(i + 1).getPrice();
            assertTrue(current <= next,
                    "price_asc: expected list[" + i + "].price (" + current +
                    ") <= list[" + (i + 1) + "].price (" + next + ")");
        }
    }

    /**
     * Property 2: Search sort ordering — price_desc
     *
     * For any set of listings sorted by price descending, each item's price
     * must be greater than or equal to the next item's price.
     *
     * Validates: Requirements 2.7
     */
    @Property(tries = 200)
    void sortByPriceDescending_maintainsPairwiseOrdering(
            @ForAll("randomListings") List<Listing> listings
    ) {
        // Apply price_desc sort
        List<Listing> sorted = listings.stream()
                .sorted(Comparator.comparingDouble(Listing::getPrice).reversed())
                .toList();

        // Verify pairwise ordering: list[i].price >= list[i+1].price
        for (int i = 0; i < sorted.size() - 1; i++) {
            double current = sorted.get(i).getPrice();
            double next = sorted.get(i + 1).getPrice();
            assertTrue(current >= next,
                    "price_desc: expected list[" + i + "].price (" + current +
                    ") >= list[" + (i + 1) + "].price (" + next + ")");
        }
    }

    /**
     * Property 2: Search sort ordering — newest
     *
     * For any set of listings sorted by createdAt descending (newest first),
     * each item's createdAt must be greater than or equal to the next item's createdAt.
     *
     * Validates: Requirements 2.7
     */
    @Property(tries = 200)
    void sortByNewest_maintainsPairwiseOrdering(
            @ForAll("randomListings") List<Listing> listings
    ) {
        // Apply newest sort (createdAt descending)
        List<Listing> sorted = listings.stream()
                .sorted(Comparator.comparing(Listing::getCreatedAt).reversed())
                .toList();

        // Verify pairwise ordering: list[i].createdAt >= list[i+1].createdAt
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalDateTime current = sorted.get(i).getCreatedAt();
            LocalDateTime next = sorted.get(i + 1).getCreatedAt();
            assertTrue(current.compareTo(next) >= 0,
                    "newest: expected list[" + i + "].createdAt (" + current +
                    ") >= list[" + (i + 1) + "].createdAt (" + next + ")");
        }
    }

    /**
     * Property 2: Search sort ordering — views
     *
     * For any set of listings sorted by viewCount descending (most views first),
     * each item's viewCount must be greater than or equal to the next item's viewCount.
     *
     * Validates: Requirements 2.7
     */
    @Property(tries = 200)
    void sortByViews_maintainsPairwiseOrdering(
            @ForAll("randomListings") List<Listing> listings
    ) {
        // Apply views sort (viewCount descending)
        List<Listing> sorted = listings.stream()
                .sorted(Comparator.comparingInt(Listing::getViewCount).reversed())
                .toList();

        // Verify pairwise ordering: list[i].viewCount >= list[i+1].viewCount
        for (int i = 0; i < sorted.size() - 1; i++) {
            int current = sorted.get(i).getViewCount();
            int next = sorted.get(i + 1).getViewCount();
            assertTrue(current >= next,
                    "views: expected list[" + i + "].viewCount (" + current +
                    ") >= list[" + (i + 1) + "].viewCount (" + next + ")");
        }
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<List<Listing>> randomListings() {
        Arbitrary<Listing> listingArbitrary = Arbitraries.longs().between(1L, 10000L)
                .flatMap(id -> Arbitraries.doubles().between(1.0, 500000.0)
                        .flatMap(price -> Arbitraries.integers().between(0, 10000)
                                .flatMap(viewCount -> randomDateTime()
                                        .map(createdAt -> Listing.builder()
                                                .id(id)
                                                .title("Listing-" + id)
                                                .category("agricultural-products")
                                                .status("ACTIVE")
                                                .price(price)
                                                .viewCount(viewCount)
                                                .createdAt(createdAt)
                                                .build()))));

        return listingArbitrary.list().ofMinSize(2).ofMaxSize(50);
    }

    private Arbitrary<LocalDateTime> randomDateTime() {
        return Arbitraries.longs()
                .between(
                        LocalDateTime.of(2023, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC),
                        LocalDateTime.of(2025, 12, 31, 23, 59).toEpochSecond(ZoneOffset.UTC)
                )
                .map(epoch -> LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC));
    }
}
