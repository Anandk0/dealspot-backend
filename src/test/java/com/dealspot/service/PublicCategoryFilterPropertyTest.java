package com.dealspot.service;

import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based test for public category filtering in CategoryService.
 *
 * Validates: Requirements 4.2, 5.1
 */
@Tag("dynamic-categories")
@Tag("public-category-filter")
class PublicCategoryFilterPropertyTest {

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    PublicCategoryFilterPropertyTest() {
        this.categoryRepository = mock(CategoryRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        AuditService auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 9: Public API Returns Only Active Categories in Sort Order.
     *
     * Given any mix of active/inactive categories, the getActiveCategories() method
     * returns only active categories sorted by sortOrder ascending.
     *
     * Validates: Requirements 4.2, 5.1
     */
    @Property(tries = 200)
    void getActiveCategories_returnsOnlyActiveCategoriesInSortOrder(
            @ForAll("mixedCategories") List<Category> allCategories
    ) {
        // Compute the expected result: only active categories, sorted by sortOrder ascending
        List<Category> expectedActive = allCategories.stream()
                .filter(Category::getActive)
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .collect(Collectors.toList());

        // Mock the repository to return the expected filtered/sorted result
        when(categoryRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(expectedActive);

        // Call the service method
        List<Category> result = categoryService.getActiveCategories();

        // 1. Result contains ONLY categories where active=true
        for (Category cat : result) {
            assertTrue(cat.getActive(),
                    "Public API should only return active categories, but found inactive: " + cat.getSlug());
        }

        // 2. Result is sorted by sortOrder ascending
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).getSortOrder() >= result.get(i - 1).getSortOrder(),
                    "Categories should be sorted by sortOrder ascending, but found "
                            + result.get(i - 1).getSortOrder() + " before " + result.get(i).getSortOrder());
        }

        // 3. No inactive categories are present (verify count matches expected)
        long activeCount = allCategories.stream().filter(Category::getActive).count();
        assertEquals(activeCount, result.size(),
                "Result size should equal number of active categories in input");

        // 4. The result matches exactly what the repository provides
        assertEquals(expectedActive, result,
                "Service should return exactly what the repository provides");
    }

    // ─── Providers ────────────────────────────────────────

    /**
     * Generates a random list of categories with a mix of active/inactive states
     * and varying sortOrder values.
     */
    @Provide
    Arbitrary<List<Category>> mixedCategories() {
        Arbitrary<Category> categoryArbitrary = Combinators.combine(
                Arbitraries.longs().between(1L, 1000L),                    // id
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10), // name
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10), // nameEn
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8),  // slug
                Arbitraries.of(true, false),                                  // active
                Arbitraries.integers().between(0, 100)                        // sortOrder
        ).as((id, name, nameEn, slug, active, sortOrder) ->
                Category.builder()
                        .id(id)
                        .name(name)
                        .nameEn(nameEn)
                        .slug(slug.toLowerCase())
                        .icon("🏷️")
                        .active(active)
                        .sortOrder(sortOrder)
                        .moderationLevel(ModerationLevel.CHECKER_ONLY)
                        .build()
        );

        return categoryArbitrary.list().ofMinSize(1).ofMaxSize(20);
    }
}
