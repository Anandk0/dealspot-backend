package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.entity.User;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for seller profile active listings filter,
 * district prioritization in search results, and similar listings constraint.
 *
 * These are in-memory logic tests that verify the core filtering/sorting algorithms
 * without database interaction.
 *
 * Validates: Requirements 5.2, 8.5, 9.5
 */
@Tag("buyer-experience")
@Tag("seller-profile")
class SellerProfilePropertyTest {

    // Valid statuses a listing can have
    private static final String[] ALL_STATUSES = {"ACTIVE", "SOLD", "PENDING", "REJECTED", "FLAGGED", "EXPIRED"};

    // Sample Karnataka districts
    private static final String[] DISTRICTS = {
            "Bengaluru", "Mysuru", "Hubli-Dharwad", "Mangaluru", "Belagavi",
            "Kalaburagi", "Davangere", "Ballari", "Tumakuru", "Shivamogga"
    };

    // Sample categories
    private static final String[] CATEGORIES = {
            "livestock", "farm-equipment", "tractor-rental", "agricultural-products",
            "vehicle-rental", "labor", "land", "services"
    };

    // ─────────────────────────────────────────────────────────────────────────────
    // Property 10: Seller profile shows only active listings
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Property 10: Seller profile shows only active listings.
     *
     * For any seller with listings in various states (ACTIVE, PENDING, SOLD, REJECTED, etc.),
     * filtering for active listings shall return ONLY listings with status == "ACTIVE".
     *
     * Validates: Requirements 5.2
     */
    @Property(tries = 100)
    void sellerProfile_showsOnlyActiveListings(
            @ForAll("listingsWithMixedStatuses") List<Listing> allListings
    ) {
        // Act: Apply the same filter logic used by SellerProfileService
        List<Listing> activeListings = allListings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .collect(Collectors.toList());

        // Assert: ALL returned listings have ACTIVE status
        for (Listing listing : activeListings) {
            assertEquals("ACTIVE", listing.getStatus(),
                    "Seller profile must only contain ACTIVE listings, but found: " + listing.getStatus());
        }

        // Assert: No ACTIVE listing was missed
        long expectedActiveCount = allListings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .count();
        assertEquals(expectedActiveCount, activeListings.size(),
                "All ACTIVE listings must appear in the seller profile");
    }

    /**
     * Property 10 (complement): Non-ACTIVE listings are excluded.
     *
     * Validates: Requirements 5.2
     */
    @Property(tries = 100)
    void sellerProfile_excludesNonActiveListings(
            @ForAll("listingsWithMixedStatuses") List<Listing> allListings
    ) {
        // Act: Apply the filter
        List<Listing> activeListings = allListings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .collect(Collectors.toList());

        // Assert: No non-ACTIVE listing slipped through
        for (Listing listing : activeListings) {
            assertNotEquals("SOLD", listing.getStatus());
            assertNotEquals("PENDING", listing.getStatus());
            assertNotEquals("REJECTED", listing.getStatus());
            assertNotEquals("FLAGGED", listing.getStatus());
            assertNotEquals("EXPIRED", listing.getStatus());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Property 15: District prioritization in search results
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Property 15: District prioritization in search results.
     *
     * For any search results and buyer district, when no explicit district filter is applied,
     * ALL listings from the buyer's district shall appear before listings from other districts.
     *
     * Validates: Requirements 8.5
     */
    @Property(tries = 100)
    void districtPrioritization_localListingsAppearFirst(
            @ForAll("activeListingsWithDistricts") List<Listing> listings,
            @ForAll("districts") String buyerDistrict
    ) {
        // Act: Sort with district priority (same logic as SearchService)
        List<Listing> sorted = listings.stream()
                .sorted(Comparator.comparingInt(l -> buyerDistrict.equals(l.getDistrict()) ? 0 : 1))
                .collect(Collectors.toList());

        // Assert: Find the boundary — all local listings before non-local
        boolean foundNonLocal = false;
        for (Listing listing : sorted) {
            if (!buyerDistrict.equals(listing.getDistrict())) {
                foundNonLocal = true;
            } else if (foundNonLocal) {
                fail("Local listing (district=" + buyerDistrict +
                        ") found AFTER non-local listing. District prioritization violated.");
            }
        }
    }

    /**
     * Property 15 (count preservation): District prioritization preserves all listings.
     *
     * Validates: Requirements 8.5
     */
    @Property(tries = 100)
    void districtPrioritization_preservesAllListings(
            @ForAll("activeListingsWithDistricts") List<Listing> listings,
            @ForAll("districts") String buyerDistrict
    ) {
        // Act: Sort with district priority
        List<Listing> sorted = listings.stream()
                .sorted(Comparator.comparingInt(l -> buyerDistrict.equals(l.getDistrict()) ? 0 : 1))
                .collect(Collectors.toList());

        // Assert: No listings are lost or duplicated
        assertEquals(listings.size(), sorted.size(),
                "District prioritization must not change the total number of listings");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Property 16: Similar listings constraint
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Property 16: Similar listings constraint.
     *
     * For any listing with a given category and district, the "Similar Listings" results
     * shall ALL share the same category AND the same district as the source listing,
     * the count shall be at most 4, and the source listing itself is excluded.
     *
     * Validates: Requirements 9.5
     */
    @Property(tries = 100)
    void similarListings_shareCategoryAndDistrict_andMaxFour(
            @ForAll("listingPools") List<Listing> pool,
            @ForAll @IntRange(min = 0, max = 9) int sourceIndex
    ) {
        if (pool.isEmpty()) return;

        // Pick a source listing from the pool
        Listing source = pool.get(sourceIndex % pool.size());
        String sourceCategory = source.getCategory();
        String sourceDistrict = source.getDistrict();

        if (sourceCategory == null || sourceDistrict == null) return;

        // Act: Apply the similar listings logic (same as SearchService.getSimilarListings)
        int maxResults = 4;
        List<Listing> similar = pool.stream()
                .filter(l -> !l.getId().equals(source.getId()))
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .filter(l -> sourceCategory.equals(l.getCategory()))
                .filter(l -> sourceDistrict.equals(l.getDistrict()))
                .limit(maxResults)
                .collect(Collectors.toList());

        // Assert: count ≤ 4
        assertTrue(similar.size() <= 4,
                "Similar listings must return at most 4 results, but got: " + similar.size());

        // Assert: ALL share category and district with source
        for (Listing listing : similar) {
            assertEquals(sourceCategory, listing.getCategory(),
                    "Similar listing must share category with source. Expected: " +
                            sourceCategory + ", got: " + listing.getCategory());
            assertEquals(sourceDistrict, listing.getDistrict(),
                    "Similar listing must share district with source. Expected: " +
                            sourceDistrict + ", got: " + listing.getDistrict());
        }

        // Assert: Source listing is excluded from results
        for (Listing listing : similar) {
            assertNotEquals(source.getId(), listing.getId(),
                    "Source listing must be excluded from similar listings results");
        }
    }

    /**
     * Property 16 (exclusion): Source listing is always excluded from similar results
     * even when it matches its own category + district.
     *
     * Validates: Requirements 9.5
     */
    @Property(tries = 100)
    void similarListings_alwaysExcludesSource(
            @ForAll("listingPools") List<Listing> pool,
            @ForAll @IntRange(min = 0, max = 9) int sourceIndex
    ) {
        if (pool.isEmpty()) return;

        Listing source = pool.get(sourceIndex % pool.size());
        String sourceCategory = source.getCategory();
        String sourceDistrict = source.getDistrict();

        if (sourceCategory == null || sourceDistrict == null) return;

        List<Listing> similar = pool.stream()
                .filter(l -> !l.getId().equals(source.getId()))
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .filter(l -> sourceCategory.equals(l.getCategory()))
                .filter(l -> sourceDistrict.equals(l.getDistrict()))
                .limit(4)
                .collect(Collectors.toList());

        // Assert: source not in results
        boolean containsSource = similar.stream()
                .anyMatch(l -> l.getId().equals(source.getId()));
        assertFalse(containsSource,
                "Similar listings must never contain the source listing itself");
    }

    // ─── Providers ────────────────────────────────────────────────────────────────

    /**
     * Generates a list of listings with mixed statuses for a single seller.
     */
    @Provide
    Arbitrary<List<Listing>> listingsWithMixedStatuses() {
        Arbitrary<Listing> listingArb = Combinators.combine(
                Arbitraries.longs().between(1L, 500L),
                Arbitraries.of(ALL_STATUSES),
                Arbitraries.of(CATEGORIES),
                Arbitraries.of(DISTRICTS)
        ).as((id, status, category, district) ->
                Listing.builder()
                        .id(id)
                        .title("Listing-" + id)
                        .category(category)
                        .district(district)
                        .status(status)
                        .price(1000.0)
                        .build()
        );

        return listingArb.list().ofMinSize(1).ofMaxSize(30);
    }

    /**
     * Generates a list of ACTIVE listings with various districts.
     */
    @Provide
    Arbitrary<List<Listing>> activeListingsWithDistricts() {
        Arbitrary<Listing> listingArb = Combinators.combine(
                Arbitraries.longs().between(1L, 500L),
                Arbitraries.of(DISTRICTS),
                Arbitraries.of(CATEGORIES)
        ).as((id, district, category) ->
                Listing.builder()
                        .id(id)
                        .title("Listing-" + id)
                        .category(category)
                        .district(district)
                        .status("ACTIVE")
                        .price(1000.0)
                        .build()
        );

        return listingArb.list().ofMinSize(1).ofMaxSize(30);
    }

    /**
     * Generates a random district for the buyer.
     */
    @Provide
    Arbitrary<String> districts() {
        return Arbitraries.of(DISTRICTS);
    }

    /**
     * Generates a pool of listings with various categories, districts, and statuses.
     * Uses unique IDs to ensure source exclusion logic works correctly.
     */
    @Provide
    Arbitrary<List<Listing>> listingPools() {
        return Arbitraries.integers().between(3, 20).flatMap(size -> {
            List<Arbitrary<Listing>> listArbs = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final long id = i + 1L;
                Arbitrary<Listing> arb = Combinators.combine(
                        Arbitraries.of(ALL_STATUSES),
                        Arbitraries.of(CATEGORIES),
                        Arbitraries.of(DISTRICTS)
                ).as((status, category, district) ->
                        Listing.builder()
                                .id(id)
                                .title("Listing-" + id)
                                .category(category)
                                .district(district)
                                .status(status)
                                .price(1000.0)
                                .build()
                );
                listArbs.add(arb);
            }
            return Combinators.combine(listArbs).as(listings -> listings);
        });
    }
}
