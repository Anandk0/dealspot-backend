package com.dealspot.service;

import com.dealspot.dto.CategoryRequest;
import com.dealspot.entity.Category;
import com.dealspot.entity.User;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for slug uniqueness enforcement on category update.
 *
 * Validates: Requirements 3.2
 */
@Tag("dynamic-categories")
@Tag("slug-uniqueness-update")
class SlugUniquenessUpdatePropertyTest {

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    SlugUniquenessUpdatePropertyTest() {
        this.categoryRepository = mock(CategoryRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        AuditService auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 4: Slug Uniqueness Enforced on Update.
     *
     * For ANY two categories with different slugs and different IDs,
     * updating one category's slug to match the other's slug — when that slug
     * is already taken — ALWAYS results in a 409 CONFLICT error.
     *
     * Validates: Requirements 3.2
     */
    @Property(tries = 200)
    void updateCategory_rejectsConflictingSlug(
            @ForAll("validSlugs") String slugA,
            @ForAll("validSlugs") String slugB,
            @ForAll("categoryIds") Long idA,
            @ForAll("categoryIds") Long idB
    ) {
        // Ensure the two slugs and IDs are distinct
        Assume.that(!slugA.equals(slugB));
        Assume.that(!idA.equals(idB));

        // Category A currently has slugA
        Category categoryA = Category.builder()
                .id(idA)
                .name("ವಿಭಾಗ A")
                .nameEn("Category A")
                .slug(slugA)
                .build();

        // Build request to update Category A's slug to slugB (which belongs to Category B)
        CategoryRequest request = new CategoryRequest();
        request.setSlug(slugB);
        request.setName("ವಿಭಾಗ A");
        request.setNameEn("Category A");

        // Mock: findById returns Category A
        when(categoryRepository.findById(idA)).thenReturn(Optional.of(categoryA));
        // Mock: existsBySlugAndIdNot returns true — slugB is taken by another category
        when(categoryRepository.existsBySlugAndIdNot(slugB, idA)).thenReturn(true);

        // Act & Assert: updating slug to a taken value must throw CONFLICT
        User actor = mock(User.class);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> categoryService.updateCategory(idA, request, actor),
                "Updating slug to '" + slugB + "' (already taken) should throw CONFLICT");

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode(),
                "Status should be 409 CONFLICT when slug conflicts on update");
        assertTrue(exception.getReason().contains("slug"),
                "Error message should mention slug conflict");
    }

    // ─── Providers ────────────────────────────────────────

    /**
     * Generates valid URL-friendly slug strings: lowercase, [a-z0-9-], no leading/trailing hyphens.
     */
    @Provide
    Arbitrary<String> validSlugs() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3).ofMaxLength(30)
                .map(s -> s.replaceAll("-{2,}", "-"))
                .filter(s -> !s.startsWith("-") && !s.endsWith("-") && !s.isEmpty());
    }

    /**
     * Generates positive category IDs.
     */
    @Provide
    Arbitrary<Long> categoryIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }
}
