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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property-based test for category creation round-trip.
 *
 * Validates: Requirements 2.1
 *
 * Property: Creating a category with random valid fields and retrieving it by slug
 * returns matching data.
 */
@Tag("dynamic-categories")
@Tag("category-creation-round-trip")
class CategoryCreationRoundTripPropertyTest {

    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final AuditService auditService;
    private final CategoryService categoryService;

    CategoryCreationRoundTripPropertyTest() {
        this.categoryRepository = mock(CategoryRepository.class);
        this.listingRepository = mock(ListingRepository.class);
        this.auditService = mock(AuditService.class);
        this.categoryService = new CategoryService(categoryRepository, listingRepository, auditService);
    }

    /**
     * Property 2: Category Creation Round-Trip
     *
     * Create a category with random valid fields and verify retrieval by slug returns matching data.
     *
     * Validates: Requirements 2.1
     */
    @Property(tries = 200)
    void createdCategory_canBeRetrievedBySlug_withMatchingFields(
            @ForAll("validCategoryRequests") CategoryRequest request
    ) {
        // Reset mocks for each trial
        reset(categoryRepository, auditService);

        // Mock: slug does not already exist
        when(categoryRepository.existsBySlug(anyString())).thenReturn(false);

        // Mock: save returns entity built from the request fields
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category toSave = invocation.getArgument(0);
            // Simulate DB assigning an ID
            toSave.setId(1L);
            return toSave;
        });

        // Create user actor
        User actor = User.builder().id(1L).name("Admin").build();

        // Act: create category
        Category created = categoryService.createCategory(request, actor);

        // Determine expected slug
        String expectedSlug;
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            expectedSlug = request.getSlug();
        } else {
            expectedSlug = categoryService.generateSlug(request.getNameEn());
        }

        // Mock: findBySlug returns the saved category
        when(categoryRepository.findBySlug(expectedSlug)).thenReturn(Optional.of(created));

        // Act: retrieve by slug
        Category retrieved = categoryService.getCategoryBySlug(expectedSlug);

        // Assert: round-trip fields match
        assertNotNull(retrieved, "Retrieved category should not be null");
        assertEquals(request.getName(), retrieved.getName(), "Name should match");
        assertEquals(request.getNameEn(), retrieved.getNameEn(), "NameEn should match");
        assertEquals(expectedSlug, retrieved.getSlug(), "Slug should match expected");
        assertEquals(request.getIcon(), retrieved.getIcon(), "Icon should match");
        assertEquals(request.getImageUrl(), retrieved.getImageUrl(), "ImageUrl should match");
        assertEquals(request.getColor(), retrieved.getColor(), "Color should match");

        // Verify sortOrder defaults to 0 if not provided
        int expectedSortOrder = request.getSortOrder() != null ? request.getSortOrder() : 0;
        assertEquals(expectedSortOrder, retrieved.getSortOrder(), "SortOrder should match");

        // Verify active defaults to true if not provided
        boolean expectedActive = request.getActive() != null ? request.getActive() : true;
        assertEquals(expectedActive, retrieved.getActive(), "Active should match");

        // Verify moderationLevel defaults to CHECKER_ONLY if not provided
        ModerationLevel expectedModeration = request.getModerationLevel() != null
                ? request.getModerationLevel() : ModerationLevel.CHECKER_ONLY;
        assertEquals(expectedModeration, retrieved.getModerationLevel(), "ModerationLevel should match");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<CategoryRequest> validCategoryRequests() {
        Arbitrary<String> names = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1).ofMaxLength(20)
                .map(s -> s + "a"); // ensure at least one alpha char

        Arbitrary<String> namesEn = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1).ofMaxLength(20)
                .map(s -> s + "a"); // ensure at least one alphanumeric char for slug generation

        Arbitrary<String> optionalSlugs = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(15)
                        .map(s -> s + "-cat") // ensure valid slug format
        );

        Arbitrary<String> optionalIcons = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.of("🏠", "🚜", "🐄", "🌾", "🔧", "📦")
        );

        Arbitrary<String> optionalImageUrls = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(5).ofMaxLength(15)
                        .map(s -> "https://img.example.com/" + s + ".png")
        );

        Arbitrary<String> optionalColors = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.of("bg-blue-100", "bg-green-100", "bg-red-100", "bg-yellow-100")
        );

        Arbitrary<Integer> optionalSortOrders = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.integers().between(0, 100)
        );

        Arbitrary<Boolean> optionalActives = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.of(true, false)
        );

        Arbitrary<ModerationLevel> optionalModerationLevels = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.of(ModerationLevel.values())
        );

        return Combinators.combine(
                names, namesEn, optionalSlugs, optionalIcons,
                optionalImageUrls, optionalColors, optionalSortOrders,
                optionalActives
        ).as((name, nameEn, slug, icon, imageUrl, color, sortOrder, active) -> {
            CategoryRequest req = new CategoryRequest();
            req.setName(name);
            req.setNameEn(nameEn);
            req.setSlug(slug);
            req.setIcon(icon);
            req.setImageUrl(imageUrl);
            req.setColor(color);
            req.setSortOrder(sortOrder);
            req.setActive(active);
            return req;
        }).flatMap(req -> optionalModerationLevels.map(ml -> {
            req.setModerationLevel(ml);
            return req;
        }));
    }
}
