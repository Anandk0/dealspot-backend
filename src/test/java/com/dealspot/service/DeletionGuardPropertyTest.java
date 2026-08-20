package com.dealspot.service;

import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.entity.User;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for deletion guard based on listing count.
 *
 * Validates: Requirements 4.3, 4.4
 *
 * Property: Categories with 0 listings can be deleted successfully;
 * categories with ≥1 listings cannot be deleted (throws 409 CONFLICT).
 */
@Tag("dynamic-categories")
@Tag("deletion-guard")
class DeletionGuardPropertyTest {

    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final AuditService auditService;
    private final CategoryService categoryService;

    DeletionGuardPropertyTest() {
        this.categoryRepository = mock(CategoryRepository.class);
        this.listingRepository = mock(ListingRepository.class);
        this.auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 8a: Deletion succeeds when listing count is 0.
     *
     * For any valid category with 0 associated listings,
     * deleteCategory completes without throwing and the category is deleted.
     *
     * Validates: Requirements 4.3
     */
    @Property(tries = 200)
    void deleteCategory_succeeds_whenListingCountIsZero(
            @ForAll("categoryIds") Long categoryId,
            @ForAll("validSlugs") String slug
    ) {
        // Reset mocks for each trial
        reset(categoryRepository, listingRepository, auditService);

        // Set up category
        Category category = Category.builder()
                .id(categoryId)
                .name("ಟೆಸ್ಟ್")
                .nameEn("Test Category")
                .slug(slug)
                .active(true)
                .sortOrder(0)
                .moderationLevel(ModerationLevel.CHECKER_ONLY)
                .build();

        // Mock: findById returns the category
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Mock: 0 listings for this category
        when(listingRepository.countByCategory(slug)).thenReturn(0L);

        // Create actor
        User actor = User.builder().id(1L).name("Admin").build();

        // Act & Assert: no exception thrown
        assertDoesNotThrow(() -> categoryService.deleteCategory(categoryId, actor),
                "Deletion should succeed when listing count is 0");

        // Verify: category was deleted
        verify(categoryRepository, times(1)).delete(category);

        // Verify: audit was logged
        verify(auditService, times(1)).audit(eq(actor), eq("DELETE_CATEGORY"),
                eq("CATEGORY"), eq(categoryId), any(String.class));
    }

    /**
     * Property 8b: Deletion fails when listing count >= 1.
     *
     * For any valid category with a positive number of associated listings (1-100),
     * deleteCategory throws ResponseStatusException with 409 CONFLICT status
     * and the category is NOT deleted.
     *
     * Validates: Requirements 4.4
     */
    @Property(tries = 200)
    void deleteCategory_throwsConflict_whenListingCountIsPositive(
            @ForAll("categoryIds") Long categoryId,
            @ForAll("validSlugs") String slug,
            @ForAll("positiveListingCounts") long listingCount
    ) {
        // Reset mocks for each trial
        reset(categoryRepository, listingRepository, auditService);

        // Set up category
        Category category = Category.builder()
                .id(categoryId)
                .name("ಟೆಸ್ಟ್")
                .nameEn("Test Category")
                .slug(slug)
                .active(true)
                .sortOrder(0)
                .moderationLevel(ModerationLevel.CHECKER_ONLY)
                .build();

        // Mock: findById returns the category
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Mock: positive listing count for this category
        when(listingRepository.countByCategory(slug)).thenReturn(listingCount);

        // Create actor
        User actor = User.builder().id(1L).name("Admin").build();

        // Act & Assert: should throw ResponseStatusException with CONFLICT status
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.deleteCategory(categoryId, actor),
                "Deletion should be rejected when listing count is " + listingCount);

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode(),
                "Should return 409 CONFLICT when category has listings");
        assertTrue(ex.getReason().contains("Cannot delete category with existing listings"),
                "Error message should explain why deletion was rejected");

        // Verify: category was NOT deleted
        verify(categoryRepository, never()).delete(any(Category.class));

        // Verify: no audit log for deletion
        verify(auditService, never()).audit(any(), eq("DELETE_CATEGORY"), any(), any(), any());
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Long> categoryIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> validSlugs() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3).ofMaxLength(15)
                .map(s -> s + "-cat");
    }

    @Provide
    Arbitrary<Long> positiveListingCounts() {
        return Arbitraries.longs().between(1L, 100L);
    }
}
