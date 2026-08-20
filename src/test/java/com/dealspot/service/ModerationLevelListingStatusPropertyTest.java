package com.dealspot.service;

import com.dealspot.dto.ListingRequest;
import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.Category;
import com.dealspot.entity.Listing;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test: Moderation Level Determines Initial Listing Status.
 *
 * Validates: Requirements A.2 (Category Control Master)
 *
 * Property 12: For any category slug and moderation level combination,
 * the initial listing status is determined by the category's moderation level:
 * - NO_AUTH → "ACTIVE"
 * - CHECKER_ONLY → "PENDING"
 * - ADMIN_AND_CHECKER → "PENDING"
 * - Category not found (exception) → "PENDING"
 */
@Tag("dynamic-categories")
@Tag("moderation-level-listing-status")
class ModerationLevelListingStatusPropertyTest {

    private final ListingRepository listingRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryService categoryService;
    private final ListingService listingService;

    ModerationLevelListingStatusPropertyTest() {
        this.listingRepository = mock(ListingRepository.class);
        this.cloudinaryService = mock(CloudinaryService.class);
        this.categoryService = mock(CategoryService.class);
        this.listingService = new ListingService(listingRepository, cloudinaryService, categoryService);
    }

    /**
     * Property 12a: NO_AUTH moderation level produces ACTIVE listing status.
     *
     * For ANY category slug where getCategoryBySlug returns a category with NO_AUTH,
     * the created listing has status "ACTIVE".
     *
     * Validates: Requirements A.2 (Category Control Master)
     */
    @Property(tries = 100)
    void createListing_setsActiveStatus_whenModerationLevelIsNoAuth(
            @ForAll("validSlugs") String slug,
            @ForAll("validTitles") String title
    ) {
        // Reset mocks for each trial
        reset(listingRepository, cloudinaryService, categoryService);

        // Set up category with NO_AUTH moderation
        Category category = Category.builder()
                .id(1L)
                .name("ಟೆಸ್ಟ್")
                .nameEn("Test")
                .slug(slug)
                .moderationLevel(ModerationLevel.NO_AUTH)
                .build();

        when(categoryService.getCategoryBySlug(slug)).thenReturn(category);

        // Mock save to return listing with an ID set
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Create request
        ListingRequest request = new ListingRequest();
        request.setTitle(title);
        request.setCategory(slug);

        // Create user
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .phone("9999999999")
                .password("pass")
                .location("Test Location")
                .build();

        // Act
        ListingResponse response = listingService.createListing(request, null, user);

        // Assert: status should be ACTIVE for NO_AUTH
        assertEquals("ACTIVE", response.getStatus(),
                "Listing status should be ACTIVE when category moderation level is NO_AUTH (slug: " + slug + ")");
    }

    /**
     * Property 12b: CHECKER_ONLY or ADMIN_AND_CHECKER moderation level produces PENDING listing status.
     *
     * For ANY category slug where getCategoryBySlug returns a category with CHECKER_ONLY
     * or ADMIN_AND_CHECKER, the created listing has status "PENDING".
     *
     * Validates: Requirements A.2 (Category Control Master)
     */
    @Property(tries = 100)
    void createListing_setPendingStatus_whenModerationLevelRequiresReview(
            @ForAll("validSlugs") String slug,
            @ForAll("validTitles") String title,
            @ForAll("reviewRequiredLevels") ModerationLevel level
    ) {
        // Reset mocks for each trial
        reset(listingRepository, cloudinaryService, categoryService);

        // Set up category with moderation level requiring review
        Category category = Category.builder()
                .id(1L)
                .name("ಟೆಸ್ಟ್")
                .nameEn("Test")
                .slug(slug)
                .moderationLevel(level)
                .build();

        when(categoryService.getCategoryBySlug(slug)).thenReturn(category);

        // Mock save to return listing with an ID set
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Create request
        ListingRequest request = new ListingRequest();
        request.setTitle(title);
        request.setCategory(slug);

        // Create user
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .phone("9999999999")
                .password("pass")
                .location("Test Location")
                .build();

        // Act
        ListingResponse response = listingService.createListing(request, null, user);

        // Assert: status should be PENDING for CHECKER_ONLY and ADMIN_AND_CHECKER
        assertEquals("PENDING", response.getStatus(),
                "Listing status should be PENDING when category moderation level is " + level + " (slug: " + slug + ")");
    }

    /**
     * Property 12c: Category not found produces PENDING listing status.
     *
     * For ANY category slug where getCategoryBySlug throws an exception,
     * the created listing defaults to status "PENDING".
     *
     * Validates: Requirements A.2 (Category Control Master)
     */
    @Property(tries = 100)
    void createListing_setPendingStatus_whenCategoryNotFound(
            @ForAll("validSlugs") String slug,
            @ForAll("validTitles") String title
    ) {
        // Reset mocks for each trial
        reset(listingRepository, cloudinaryService, categoryService);

        // Category lookup throws exception (not found)
        when(categoryService.getCategoryBySlug(slug))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Category not found"));

        // Mock save to return listing with an ID set
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Create request
        ListingRequest request = new ListingRequest();
        request.setTitle(title);
        request.setCategory(slug);

        // Create user
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .phone("9999999999")
                .password("pass")
                .location("Test Location")
                .build();

        // Act
        ListingResponse response = listingService.createListing(request, null, user);

        // Assert: status should be PENDING when category not found
        assertEquals("PENDING", response.getStatus(),
                "Listing status should default to PENDING when category is not found (slug: " + slug + ")");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<String> validSlugs() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3).ofMaxLength(20)
                .map(s -> s + "-category");
    }

    @Provide
    Arbitrary<String> validTitles() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3).ofMaxLength(30)
                .map(s -> "Listing " + s);
    }

    @Provide
    Arbitrary<ModerationLevel> reviewRequiredLevels() {
        return Arbitraries.of(ModerationLevel.CHECKER_ONLY, ModerationLevel.ADMIN_AND_CHECKER);
    }
}
