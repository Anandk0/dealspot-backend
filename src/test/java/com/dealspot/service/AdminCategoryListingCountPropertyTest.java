package com.dealspot.service;

import com.dealspot.dto.CategoryAdminResponse;
import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for admin API returning all categories with correct listing counts.
 *
 * Validates: Requirements 6.1, 6.2
 *
 * Property: For ANY random list of categories (mix of active/inactive) with arbitrary
 * listing counts, getAllCategoriesAdmin() returns ALL categories (active and inactive)
 * with the correct listing count for each.
 */
@Tag("dynamic-categories")
@Tag("admin-category-listing-count")
class AdminCategoryListingCountPropertyTest {

    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final AuditService auditService;
    private final CategoryService categoryService;

    AdminCategoryListingCountPropertyTest() {
        this.categoryRepository = mock(CategoryRepository.class);
        this.listingRepository = mock(ListingRepository.class);
        this.auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 10: Admin API Returns All Categories with Correct Listing Count
     *
     * For any generated list of categories (active and inactive) with random listing counts,
     * calling getAllCategoriesAdmin() must:
     * 1. Return ALL categories (both active and inactive)
     * 2. Each CategoryAdminResponse has the correct listing count matching countByCategory
     * 3. Result size matches input size
     *
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 200)
    void getAllCategoriesAdmin_returnsAllWithCorrectListingCounts(
            @ForAll("categoryCountAndActivity") List<Boolean> activeFlags,
            @ForAll("listingCountsList") List<Integer> listingCounts
    ) {
        // Ensure both lists have the same effective size (use smaller)
        int size = Math.min(activeFlags.size(), listingCounts.size());
        Assume.that(size >= 1);

        // Reset mocks for each trial
        reset(categoryRepository, listingRepository);

        // Build categories with generated active flags
        List<Category> categories = IntStream.range(0, size)
                .mapToObj(i -> Category.builder()
                        .id((long) (i + 1))
                        .name("ವಿಭಾಗ " + i)
                        .nameEn("Category " + i)
                        .slug("category-" + i)
                        .icon("🏷️")
                        .color("bg-blue-100")
                        .active(activeFlags.get(i))
                        .sortOrder(i)
                        .moderationLevel(ModerationLevel.CHECKER_ONLY)
                        .build())
                .collect(Collectors.toList());

        // Map slug -> listing count
        Map<String, Long> slugToCount = new HashMap<>();
        for (int i = 0; i < size; i++) {
            slugToCount.put("category-" + i, listingCounts.get(i).longValue());
        }

        // Mock repository: return all categories regardless of active status
        when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(categories);

        // Mock listingRepository.countByCategory for each slug
        for (int i = 0; i < size; i++) {
            when(listingRepository.countByCategory("category-" + i))
                    .thenReturn(listingCounts.get(i).longValue());
        }

        // Act
        List<CategoryAdminResponse> result = categoryService.getAllCategoriesAdmin();

        // Assert 1: Result size matches input size (all categories returned)
        assertEquals(size, result.size(),
                "Admin response must return ALL categories (active and inactive)");

        // Assert 2: Each response has the correct listing count
        for (int i = 0; i < size; i++) {
            CategoryAdminResponse response = result.get(i);
            long expectedCount = listingCounts.get(i).longValue();

            assertEquals(expectedCount, response.getListingCount(),
                    "Listing count for category '" + response.getSlug() + "' should match repository count");
        }

        // Assert 3: Both active and inactive categories are present
        long activeInResult = result.stream().filter(CategoryAdminResponse::getActive).count();
        long inactiveInResult = result.stream().filter(r -> !r.getActive()).count();
        long expectedActive = activeFlags.subList(0, size).stream().filter(b -> b).count();
        long expectedInactive = size - expectedActive;

        assertEquals(expectedActive, activeInResult,
                "Number of active categories in result must match input");
        assertEquals(expectedInactive, inactiveInResult,
                "Number of inactive categories in result must match input");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<List<Boolean>> categoryCountAndActivity() {
        return Arbitraries.of(true, false).list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<Integer>> listingCountsList() {
        return Arbitraries.integers().between(0, 100).list().ofMinSize(1).ofMaxSize(20);
    }
}
