package com.dealspot.service;

import com.dealspot.entity.Listing;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for discovery feed filtering and sorting logic.
 *
 * Property 3: Discovery nearby listings filter
 * Property 4: Discovery featured listings filter
 * Property 5: Discovery trending sort
 *
 * These test the filter/sort LOGIC in memory, mirroring what SearchService does
 * via JPA queries (getNearbyListings, getFeaturedListings, getTrendingListings).
 *
 * Validates: Requirements 1.4, 1.5, 1.6
 */
@Tag("buyer-experience")
@Tag("discovery-feeds")
class DiscoveryFeedPropertyTest {

    private static final String[] DISTRICTS = {
            "Bengaluru", "Mysuru", "Hubballi", "Belagavi", "Mangaluru",
            "Dharwad", "Davangere", "Tumakuru", "Shivamogga", "Raichur"
    };

    private static final String[] STATUSES = {
            "ACTIVE", "PENDING", "REJECTED", "SOLD", "EXPIRED", "FLAGGED"
    };

    // ─── Property 3: Discovery nearby listings filter ───────────────────────────

    /**
     * Property 3: For any buyer with a set district and any set of listings across
     * multiple districts, the "Nearby Listings" results SHALL contain ONLY listings
     * whose district matches the buyer's district AND whose status is ACTIVE.
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void nearbyListings_containOnly_matchingDistrictAndActiveStatus(
            @ForAll("listings") @Size(min = 1, max = 50) List<Listing> listings,
            @ForAll("districts") String targetDistrict
    ) {
        // Apply nearby filter logic (same as SearchService.getNearbyListings)
        List<Listing> results = getNearbyListingsInMemory(listings, targetDistrict);

        // Property: Every result must have matching district AND ACTIVE status
        for (Listing listing : results) {
            assertEquals("ACTIVE", listing.getStatus(),
                    "Nearby listing must have ACTIVE status, got: " + listing.getStatus());
            assertEquals(targetDistrict, listing.getDistrict(),
                    "Nearby listing must have district=" + targetDistrict
                            + ", got: " + listing.getDistrict());
        }
    }

    /**
     * Property 3 (completeness): Every ACTIVE listing with matching district
     * MUST appear in the nearby results — no valid listing is excluded.
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void nearbyListings_includeAll_activeListingsInTargetDistrict(
            @ForAll("listings") @Size(min = 1, max = 50) List<Listing> listings,
            @ForAll("districts") String targetDistrict
    ) {
        List<Listing> results = getNearbyListingsInMemory(listings, targetDistrict);

        // Count expected matches
        long expectedCount = listings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()) && targetDistrict.equals(l.getDistrict()))
                .count();

        assertEquals(expectedCount, results.size(),
                "Nearby results must include exactly all ACTIVE listings in district " + targetDistrict);
    }

    // ─── Property 4: Discovery featured listings filter ─────────────────────────

    /**
     * Property 4: For any set of listings with varying featured flags, the "Featured
     * Listings" results SHALL contain ONLY listings marked as featured=true AND
     * with status ACTIVE.
     *
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void featuredListings_containOnly_featuredAndActiveListings(
            @ForAll("listingsWithFeatured") @Size(min = 1, max = 50) List<Listing> listings
    ) {
        // Apply featured filter logic (same as SearchService.getFeaturedListings)
        List<Listing> results = getFeaturedListingsInMemory(listings);

        // Property: Every result must have featured=true AND status=ACTIVE
        for (Listing listing : results) {
            assertEquals("ACTIVE", listing.getStatus(),
                    "Featured listing must have ACTIVE status, got: " + listing.getStatus());
            assertTrue(listing.getFeatured(),
                    "Featured listing must have featured=true");
        }
    }

    /**
     * Property 4 (completeness): Every ACTIVE listing with featured=true
     * MUST appear in the featured results.
     *
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void featuredListings_includeAll_activeFeaturedListings(
            @ForAll("listingsWithFeatured") @Size(min = 1, max = 50) List<Listing> listings
    ) {
        List<Listing> results = getFeaturedListingsInMemory(listings);

        long expectedCount = listings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()) && Boolean.TRUE.equals(l.getFeatured()))
                .count();

        assertEquals(expectedCount, results.size(),
                "Featured results must include exactly all ACTIVE+featured listings");
    }

    // ─── Property 5: Discovery trending sort ────────────────────────────────────

    /**
     * Property 5: For any set of active listings, the "Trending" results SHALL return
     * only listings created within the last 7 days with ACTIVE status, sorted by
     * viewCount in descending order.
     *
     * Validates: Requirements 1.6
     */
    @Property(tries = 100)
    void trendingListings_containOnly_recentActiveListings_sortedByViewCountDesc(
            @ForAll("listingsWithDatesAndViews") @Size(min = 1, max = 50) List<Listing> listings
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // Apply trending filter logic (same as SearchService.getTrendingListings)
        List<Listing> results = getTrendingListingsInMemory(listings, sevenDaysAgo);

        // Property 1: Every result must be ACTIVE
        for (Listing listing : results) {
            assertEquals("ACTIVE", listing.getStatus(),
                    "Trending listing must have ACTIVE status, got: " + listing.getStatus());
        }

        // Property 2: Every result must have createdAt >= 7 days ago
        for (Listing listing : results) {
            assertNotNull(listing.getCreatedAt(),
                    "Trending listing must have a createdAt timestamp");
            assertFalse(listing.getCreatedAt().isBefore(sevenDaysAgo),
                    "Trending listing createdAt " + listing.getCreatedAt()
                            + " must be >= " + sevenDaysAgo);
        }

        // Property 3: Results must be sorted by viewCount DESC
        for (int i = 0; i < results.size() - 1; i++) {
            int currentViews = results.get(i).getViewCount() != null ? results.get(i).getViewCount() : 0;
            int nextViews = results.get(i + 1).getViewCount() != null ? results.get(i + 1).getViewCount() : 0;
            assertTrue(currentViews >= nextViews,
                    "Trending results must be sorted by viewCount DESC: "
                            + currentViews + " should be >= " + nextViews);
        }
    }

    /**
     * Property 5 (completeness): Every ACTIVE listing created within the last 7 days
     * MUST appear in the trending results.
     *
     * Validates: Requirements 1.6
     */
    @Property(tries = 100)
    void trendingListings_includeAll_recentActiveListings(
            @ForAll("listingsWithDatesAndViews") @Size(min = 1, max = 50) List<Listing> listings
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        List<Listing> results = getTrendingListingsInMemory(listings, sevenDaysAgo);

        long expectedCount = listings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(sevenDaysAgo))
                .count();

        assertEquals(expectedCount, results.size(),
                "Trending results must include exactly all ACTIVE listings from last 7 days");
    }

    // ─── In-memory filter/sort logic (mirrors SearchService queries) ────────────

    /**
     * Mirrors SearchService.getNearbyListings: returns ACTIVE listings matching the district.
     */
    private List<Listing> getNearbyListingsInMemory(List<Listing> listings, String district) {
        return listings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .filter(l -> district.equals(l.getDistrict()))
                .collect(Collectors.toList());
    }

    /**
     * Mirrors SearchService.getFeaturedListings: returns ACTIVE listings with featured=true.
     */
    private List<Listing> getFeaturedListingsInMemory(List<Listing> listings) {
        return listings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .filter(l -> Boolean.TRUE.equals(l.getFeatured()))
                .collect(Collectors.toList());
    }

    /**
     * Mirrors SearchService.getTrendingListings: returns ACTIVE listings from the last 7 days,
     * sorted by viewCount DESC.
     */
    private List<Listing> getTrendingListingsInMemory(List<Listing> listings, LocalDateTime since) {
        return listings.stream()
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(since))
                .sorted(Comparator.comparingInt(
                        (Listing l) -> l.getViewCount() != null ? l.getViewCount() : 0
                ).reversed())
                .collect(Collectors.toList());
    }

    // ─── Generators ────────────────────────────────────────

    @Provide
    Arbitrary<List<Listing>> listings() {
        return listingArbitrary().list();
    }

    @Provide
    Arbitrary<List<Listing>> listingsWithFeatured() {
        return listingWithFeaturedArbitrary().list();
    }

    @Provide
    Arbitrary<List<Listing>> listingsWithDatesAndViews() {
        return listingWithDatesAndViewsArbitrary().list();
    }

    @Provide
    Arbitrary<String> districts() {
        return Arbitraries.of(DISTRICTS);
    }

    /**
     * Generates random listings with varying districts and statuses.
     */
    private Arbitrary<Listing> listingArbitrary() {
        Arbitrary<String> districtArb = Arbitraries.of(DISTRICTS);
        Arbitrary<String> statusArb = Arbitraries.of(STATUSES);

        return Combinators.combine(districtArb, statusArb)
                .as((district, status) -> {
                    Listing listing = new Listing();
                    listing.setDistrict(district);
                    listing.setStatus(status);
                    listing.setTitle("Test Listing");
                    listing.setCategory("livestock");
                    listing.setFeatured(false);
                    listing.setViewCount(0);
                    listing.setCreatedAt(LocalDateTime.now());
                    return listing;
                });
    }

    /**
     * Generates random listings with varying featured flags and statuses.
     */
    private Arbitrary<Listing> listingWithFeaturedArbitrary() {
        Arbitrary<String> districtArb = Arbitraries.of(DISTRICTS);
        Arbitrary<String> statusArb = Arbitraries.of(STATUSES);
        Arbitrary<Boolean> featuredArb = Arbitraries.of(true, false);

        return Combinators.combine(districtArb, statusArb, featuredArb)
                .as((district, status, featured) -> {
                    Listing listing = new Listing();
                    listing.setDistrict(district);
                    listing.setStatus(status);
                    listing.setFeatured(featured);
                    listing.setTitle("Test Listing");
                    listing.setCategory("livestock");
                    listing.setViewCount(0);
                    listing.setCreatedAt(LocalDateTime.now());
                    return listing;
                });
    }

    /**
     * Generates random listings with varying dates (some within 7 days, some older)
     * and random viewCounts to test trending sort.
     */
    private Arbitrary<Listing> listingWithDatesAndViewsArbitrary() {
        Arbitrary<String> districtArb = Arbitraries.of(DISTRICTS);
        Arbitrary<String> statusArb = Arbitraries.of(STATUSES);
        Arbitrary<Integer> viewCountArb = Arbitraries.integers().between(0, 10000);
        // Generate dates: some within 7 days, some older (up to 30 days ago)
        Arbitrary<Integer> daysAgoArb = Arbitraries.integers().between(0, 30);

        return Combinators.combine(districtArb, statusArb, viewCountArb, daysAgoArb)
                .as((district, status, viewCount, daysAgo) -> {
                    Listing listing = new Listing();
                    listing.setDistrict(district);
                    listing.setStatus(status);
                    listing.setViewCount(viewCount);
                    listing.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
                    listing.setFeatured(false);
                    listing.setTitle("Test Listing");
                    listing.setCategory("livestock");
                    return listing;
                });
    }
}
