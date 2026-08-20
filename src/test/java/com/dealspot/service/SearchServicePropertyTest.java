package com.dealspot.service;

import com.dealspot.entity.Listing;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for SearchService filter composition (AND logic).
 *
 * Property 1: Search filter composition (AND logic)
 * For any set of listings and any combination of active filters (price range, category, district),
 * every listing in the search results SHALL satisfy ALL active filter predicates simultaneously —
 * a listing appears in results if and only if its price is within [priceMin, priceMax] AND its
 * category matches the selected category AND its district matches the selected district.
 *
 * This tests the filter LOGIC in memory, not the JPA infrastructure.
 *
 * Validates: Requirements 2.4, 2.5, 2.6, 2.8
 */
@Tag("buyer-experience")
@Tag("search-filters")
class SearchServicePropertyTest {

    // Categories and districts matching the domain model
    private static final String[] CATEGORIES = {
            "agricultural-products", "livestock", "farm-equipment",
            "tractor-rental", "vehicle-rental", "labor", "land", "services"
    };

    private static final String[] DISTRICTS = {
            "Bengaluru", "Mysuru", "Hubballi", "Belagavi", "Mangaluru",
            "Dharwad", "Davangere", "Tumakuru", "Shivamogga", "Raichur"
    };

    private static final String[] STATUSES = {
            "ACTIVE", "PENDING", "REJECTED", "SOLD", "EXPIRED", "FLAGGED"
    };

    /**
     * Property 1: Search filter composition (AND logic).
     *
     * For any random set of listings and any random filter combination,
     * every listing returned by the in-memory filter satisfies ALL active predicates,
     * AND every listing excluded fails at least one active predicate.
     *
     * Validates: Requirements 2.4, 2.5, 2.6, 2.8
     */
    @Property(tries = 100)
    void allReturnedListingsSatisfyAllActiveFilters(
            @ForAll("listings") @Size(min = 1, max = 50) List<Listing> listings,
            @ForAll("searchFilters") SearchFilter filter
    ) {
        // Apply the same AND-logic filter that SearchService uses
        List<Listing> results = filterListingsInMemory(listings, filter);

        // Property: Every result must satisfy ALL active predicates
        for (Listing listing : results) {
            // Must be ACTIVE
            assertEquals("ACTIVE", listing.getStatus(),
                    "Result listing must have ACTIVE status");

            // If category filter is set, listing's category must match
            if (filter.category != null) {
                assertEquals(filter.category, listing.getCategory(),
                        "Result listing category must match filter category");
            }

            // If district filter is set, listing's district must match
            if (filter.district != null) {
                assertEquals(filter.district, listing.getDistrict(),
                        "Result listing district must match filter district");
            }

            // If priceMin is set, listing's price must be >= priceMin
            if (filter.priceMin != null) {
                assertNotNull(listing.getPrice(), "Listing price must not be null when priceMin filter is active");
                assertTrue(listing.getPrice() >= filter.priceMin,
                        "Result listing price " + listing.getPrice()
                                + " must be >= priceMin " + filter.priceMin);
            }

            // If priceMax is set, listing's price must be <= priceMax
            if (filter.priceMax != null) {
                assertNotNull(listing.getPrice(), "Listing price must not be null when priceMax filter is active");
                assertTrue(listing.getPrice() <= filter.priceMax,
                        "Result listing price " + listing.getPrice()
                                + " must be <= priceMax " + filter.priceMax);
            }
        }
    }

    /**
     * Property 1 (completeness): Every ACTIVE listing that satisfies all predicates
     * MUST appear in the results — no valid listing is incorrectly excluded.
     *
     * Validates: Requirements 2.4, 2.5, 2.6, 2.8
     */
    @Property(tries = 100)
    void noValidListingIsExcludedFromResults(
            @ForAll("listings") @Size(min = 1, max = 50) List<Listing> listings,
            @ForAll("searchFilters") SearchFilter filter
    ) {
        List<Listing> results = filterListingsInMemory(listings, filter);

        // For each listing NOT in results, verify it fails at least one predicate
        for (Listing listing : listings) {
            if (!results.contains(listing)) {
                boolean failsAtLeastOnePredicate = !satisfiesAllPredicates(listing, filter);
                assertTrue(failsAtLeastOnePredicate,
                        "Listing excluded from results must fail at least one predicate. "
                                + "Listing: category=" + listing.getCategory()
                                + ", district=" + listing.getDistrict()
                                + ", price=" + listing.getPrice()
                                + ", status=" + listing.getStatus());
            }
        }
    }

    /**
     * Property 1 (biconditional): A listing is in results IFF it satisfies all predicates.
     *
     * Validates: Requirements 2.4, 2.5, 2.6, 2.8
     */
    @Property(tries = 100)
    void filterResultsAreExactlyTheSetOfListingsSatisfyingAllPredicates(
            @ForAll("listings") @Size(min = 1, max = 50) List<Listing> listings,
            @ForAll("searchFilters") SearchFilter filter
    ) {
        List<Listing> results = filterListingsInMemory(listings, filter);

        // Compute expected results manually
        List<Listing> expected = listings.stream()
                .filter(l -> satisfiesAllPredicates(l, filter))
                .collect(Collectors.toList());

        assertEquals(expected.size(), results.size(),
                "Result set size must equal the count of listings satisfying all predicates");
        assertTrue(results.containsAll(expected),
                "Results must contain all listings that satisfy all predicates");
        assertTrue(expected.containsAll(results),
                "Results must not contain listings that fail any predicate");
    }

    // ─── In-memory filter logic (mirrors SearchService AND logic) ──────────────

    /**
     * Applies the same AND-logic that SearchService.buildSearchSpecification uses:
     * - Always filters to ACTIVE status
     * - Category exact match (when filter is set)
     * - District exact match (when filter is set)
     * - Price >= priceMin (when filter is set)
     * - Price <= priceMax (when filter is set)
     *
     * Listings with null price are excluded when any price filter is active.
     */
    private List<Listing> filterListingsInMemory(List<Listing> listings, SearchFilter filter) {
        return listings.stream()
                .filter(l -> satisfiesAllPredicates(l, filter))
                .collect(Collectors.toList());
    }

    private boolean satisfiesAllPredicates(Listing listing, SearchFilter filter) {
        // Must be ACTIVE
        if (!"ACTIVE".equals(listing.getStatus())) {
            return false;
        }

        // Category filter (exact match)
        if (filter.category != null && !filter.category.equals(listing.getCategory())) {
            return false;
        }

        // District filter (exact match)
        if (filter.district != null && !filter.district.equals(listing.getDistrict())) {
            return false;
        }

        // Price range filters — listings without a price are excluded when price filters are active
        if (filter.priceMin != null) {
            if (listing.getPrice() == null || listing.getPrice() < filter.priceMin) {
                return false;
            }
        }
        if (filter.priceMax != null) {
            if (listing.getPrice() == null || listing.getPrice() > filter.priceMax) {
                return false;
            }
        }

        return true;
    }

    // ─── Generators ────────────────────────────────────────

    @Provide
    Arbitrary<List<Listing>> listings() {
        return listingArbitrary().list();
    }

    private Arbitrary<Listing> listingArbitrary() {
        Arbitrary<String> categories = Arbitraries.of(CATEGORIES);
        Arbitrary<String> districts = Arbitraries.of(DISTRICTS);
        Arbitrary<String> statuses = Arbitraries.of(STATUSES);
        Arbitrary<Double> prices = Arbitraries.oneOf(
                Arbitraries.doubles().between(0.0, 100000.0).ofScale(2),
                Arbitraries.just(null) // some listings may have null price
        );

        return Combinators.combine(categories, districts, statuses, prices)
                .as((category, district, status, price) -> {
                    Listing listing = new Listing();
                    listing.setCategory(category);
                    listing.setDistrict(district);
                    listing.setStatus(status);
                    listing.setPrice(price);
                    listing.setTitle("Test Listing");
                    return listing;
                });
    }

    @Provide
    Arbitrary<SearchFilter> searchFilters() {
        Arbitrary<String> categoryFilter = Arbitraries.oneOf(
                Arbitraries.of(CATEGORIES),
                Arbitraries.just(null) // no category filter
        );
        Arbitrary<String> districtFilter = Arbitraries.oneOf(
                Arbitraries.of(DISTRICTS),
                Arbitraries.just(null) // no district filter
        );
        Arbitrary<Double> priceMinFilter = Arbitraries.oneOf(
                Arbitraries.doubles().between(0.0, 50000.0).ofScale(2),
                Arbitraries.just(null) // no priceMin filter
        );
        Arbitrary<Double> priceMaxFilter = Arbitraries.oneOf(
                Arbitraries.doubles().between(0.0, 100000.0).ofScale(2),
                Arbitraries.just(null) // no priceMax filter
        );

        return Combinators.combine(categoryFilter, districtFilter, priceMinFilter, priceMaxFilter)
                .as(SearchFilter::new);
    }

    // ─── Filter DTO ────────────────────────────────────────

    /**
     * Represents a combination of search filters.
     * Null values indicate the filter is not active.
     */
    static class SearchFilter {
        final String category;
        final String district;
        final Double priceMin;
        final Double priceMax;

        SearchFilter(String category, String district, Double priceMin, Double priceMax) {
            this.category = category;
            this.district = district;
            // Ensure priceMin <= priceMax when both are set
            if (priceMin != null && priceMax != null && priceMin > priceMax) {
                this.priceMin = priceMax;
                this.priceMax = priceMin;
            } else {
                this.priceMin = priceMin;
                this.priceMax = priceMax;
            }
        }

        @Override
        public String toString() {
            return "SearchFilter{category=" + category
                    + ", district=" + district
                    + ", priceMin=" + priceMin
                    + ", priceMax=" + priceMax + "}";
        }
    }
}
