package com.dealspot.service;

import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Property-based test for slug generation in CategoryService.
 *
 * Validates: Requirements 1.4
 */
@Tag("dynamic-categories")
@Tag("slug-generation")
class SlugGenerationPropertyTest {

    private final CategoryService categoryService;

    SlugGenerationPropertyTest() {
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        AuditService auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 1: Slug Generation Produces URL-Safe Identifiers.
     *
     * For any non-empty string containing at least one alphanumeric character,
     * the generated slug must be:
     * - Non-empty
     * - Lowercase
     * - Containing only [a-z0-9-]
     * - Not starting with a hyphen
     * - Not ending with a hyphen
     * - Deterministic (same input produces same output)
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 200)
    void slugGeneration_producesUrlSafeIdentifiers(
            @ForAll("stringsWithAlphanumeric") String input
    ) {
        String slug = categoryService.generateSlug(input);

        // 1. Result is non-empty (input contains at least one alphanumeric char)
        assertFalse(slug.isEmpty(),
                "Slug should not be empty for input containing alphanumeric characters: '" + input + "'");

        // 2. Result is lowercase
        assertEquals(slug.toLowerCase(), slug,
                "Slug should be entirely lowercase: '" + slug + "'");

        // 3. Contains only characters matching [a-z0-9-]
        assertTrue(slug.matches("[a-z0-9-]+"),
                "Slug should only contain [a-z0-9-], got: '" + slug + "'");

        // 4. Doesn't start with a hyphen
        assertFalse(slug.startsWith("-"),
                "Slug should not start with a hyphen: '" + slug + "'");

        // 5. Doesn't end with a hyphen
        assertFalse(slug.endsWith("-"),
                "Slug should not end with a hyphen: '" + slug + "'");

        // 6. Deterministic (same input → same output)
        String slug2 = categoryService.generateSlug(input);
        assertEquals(slug, slug2,
                "Slug generation should be deterministic for input: '" + input + "'");
    }

    // ─── Providers ────────────────────────────────────────

    /**
     * Generates arbitrary strings that contain at least one alphanumeric character.
     * This ensures the slug will be non-empty after processing.
     */
    @Provide
    Arbitrary<String> stringsWithAlphanumeric() {
        // Generate a string that always contains at least one [a-zA-Z0-9] character
        Arbitrary<String> alphanumChar = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1).ofMaxLength(1);

        Arbitrary<String> prefix = Arbitraries.strings()
                .withCharRange(' ', '~') // printable ASCII
                .ofMinLength(0).ofMaxLength(20);

        Arbitrary<String> suffix = Arbitraries.strings()
                .withCharRange(' ', '~')
                .ofMinLength(0).ofMaxLength(20);

        return Combinators.combine(prefix, alphanumChar, suffix)
                .as((p, c, s) -> p + c + s);
    }
}
