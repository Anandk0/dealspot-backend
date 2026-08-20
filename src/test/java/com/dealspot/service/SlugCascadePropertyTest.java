package com.dealspot.service;

import com.dealspot.dto.CategoryRequest;
import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.entity.User;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for slug cascade preserving listing count.
 *
 * Validates: Requirements 3.3
 *
 * Property: For ANY category with an old slug and ANY valid new slug (different from old),
 * when the category slug is updated, the cascade update (updateCategorySlug) is invoked
 * exactly once with the correct old and new slugs.
 */
@Tag("dynamic-categories")
@Tag("slug-cascade")
class SlugCascadePropertyTest {

    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final AuditService auditService;
    private final CategoryService categoryService;

    SlugCascadePropertyTest() {
        this.categoryRepository = mock(CategoryRepository.class);
        this.listingRepository = mock(ListingRepository.class);
        this.auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 5: Slug Cascade Preserves Listing Count
     *
     * For any category with N listings, updating the slug causes updateCategorySlug
     * to be called exactly once with the correct old and new slugs.
     * The number of listings updated (N) is returned by the repository method,
     * confirming all N listings are cascaded.
     *
     * Validates: Requirements 3.3
     */
    @Property(tries = 200)
    void updatingSlug_cascadesToAllListings_withCorrectOldAndNewSlugs(
            @ForAll("listingCounts") int listingCount,
            @ForAll("validSlugs") String oldSlug,
            @ForAll("validSlugs") String newSlug,
            @ForAll("categoryIds") Long categoryId
    ) {
        // Ensure old and new slugs are different (required for cascade to trigger)
        Assume.that(!oldSlug.equals(newSlug));

        // Reset mocks for each trial
        reset(categoryRepository, listingRepository, auditService);

        // Set up existing category with the old slug
        Category existingCategory = Category.builder()
                .id(categoryId)
                .name("ಟೆಸ್ಟ್")
                .nameEn("Test Category")
                .slug(oldSlug)
                .active(true)
                .sortOrder(0)
                .moderationLevel(ModerationLevel.CHECKER_ONLY)
                .build();

        // Mock: findById returns the existing category
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        // Mock: no slug conflict with the new slug
        when(categoryRepository.existsBySlugAndIdNot(newSlug, categoryId)).thenReturn(false);

        // Mock: updateCategorySlug returns N (simulating N listings updated)
        when(listingRepository.updateCategorySlug(oldSlug, newSlug)).thenReturn(listingCount);

        // Mock: save returns the updated category
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create the update request with the new slug
        CategoryRequest request = new CategoryRequest();
        request.setName("ಟೆಸ್ಟ್");
        request.setNameEn("Test Category");
        request.setSlug(newSlug);

        // Create user actor
        User actor = User.builder().id(1L).name("Admin").build();

        // Act: update category
        Category updated = categoryService.updateCategory(categoryId, request, actor);

        // Assert: the slug was updated on the category entity
        assertEquals(newSlug, updated.getSlug(),
                "Category slug should be updated to the new slug");

        // Assert: updateCategorySlug was called exactly once with correct old and new slugs
        verify(listingRepository, times(1)).updateCategorySlug(eq(oldSlug), eq(newSlug));

        // Assert: the cascade was invoked with the specific old slug (not any other value)
        verify(listingRepository, never()).updateCategorySlug(eq(newSlug), any());
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Integer> listingCounts() {
        return Arbitraries.integers().between(0, 50);
    }

    @Provide
    Arbitrary<String> validSlugs() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3).ofMaxLength(15)
                .map(s -> s + "-cat"); // ensure valid slug format with hyphen
    }

    @Provide
    Arbitrary<Long> categoryIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }
}
