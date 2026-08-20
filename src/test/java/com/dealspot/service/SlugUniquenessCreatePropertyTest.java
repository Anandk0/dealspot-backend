package com.dealspot.service;

import com.dealspot.dto.CategoryRequest;
import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.entity.User;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for slug uniqueness enforcement on category creation.
 *
 * Validates: Requirements 1.2, 2.2
 */
@Tag("dynamic-categories")
@Tag("slug-uniqueness-create")
class SlugUniquenessCreatePropertyTest {

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    SlugUniquenessCreatePropertyTest() {
        this.categoryRepository = mock(CategoryRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        AuditService auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 3: Slug Uniqueness Enforced on Create.
     *
     * For ANY valid slug S, if the repository reports that a category with slug S
     * already exists (existsBySlug returns true), then createCategory must throw
     * a ResponseStatusException with HTTP 409 CONFLICT status.
     *
     * Validates: Requirements 1.2, 2.2
     */
    @Property(tries = 200)
    void createCategory_throwsConflict_whenSlugAlreadyExists(
            @ForAll("validSlugs") String slug
    ) {
        // Setup: repository says this slug already exists
        when(categoryRepository.existsBySlug(slug)).thenReturn(true);

        // Build a request with the explicit slug
        CategoryRequest request = new CategoryRequest();
        request.setName("ಟೆಸ್ಟ್");
        request.setNameEn("Test Category");
        request.setSlug(slug);

        User actor = User.builder()
                .id(1L)
                .phone("9999999999")
                .password("hashed")
                .name("Admin")
                .role("ADMIN")
                .build();

        // Act & Assert: creating with a duplicate slug must throw CONFLICT
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.createCategory(request, actor),
                "Expected CONFLICT for duplicate slug: '" + slug + "'");

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode(),
                "Status should be 409 CONFLICT for slug: '" + slug + "'");
        assertTrue(ex.getReason().contains("slug already exists"),
                "Error message should mention slug conflict, got: " + ex.getReason());
    }

    /**
     * Complementary property: when existsBySlug returns false, creation succeeds
     * (no conflict thrown). This confirms the uniqueness check is the sole gate.
     *
     * Validates: Requirements 1.2, 2.2
     */
    @Property(tries = 200)
    void createCategory_succeeds_whenSlugIsUnique(
            @ForAll("validSlugs") String slug
    ) {
        // Setup: repository says slug does NOT exist, and save returns entity
        when(categoryRepository.existsBySlug(slug)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(1L);
            return cat;
        });

        CategoryRequest request = new CategoryRequest();
        request.setName("ಟೆಸ್ಟ್");
        request.setNameEn("Test Category");
        request.setSlug(slug);

        User actor = User.builder()
                .id(1L)
                .phone("9999999999")
                .password("hashed")
                .name("Admin")
                .role("ADMIN")
                .build();

        // Act: should NOT throw
        Category result = assertDoesNotThrow(
                () -> categoryService.createCategory(request, actor),
                "Should succeed when slug is unique: '" + slug + "'");

        assertNotNull(result, "Created category should not be null");
        assertEquals(slug, result.getSlug(),
                "Created category should have the requested slug");
    }

    // ─── Providers ────────────────────────────────────────

    /**
     * Generates valid slug strings: lowercase alphanumeric with hyphens,
     * not starting/ending with hyphen, 1-30 chars long.
     */
    @Provide
    Arbitrary<String> validSlugs() {
        // Generate slugs that are already URL-safe: [a-z0-9] with optional hyphens between segments
        Arbitrary<String> segment = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(10);

        return segment.list().ofMinSize(1).ofMaxSize(3)
                .map(segments -> String.join("-", segments));
    }
}
