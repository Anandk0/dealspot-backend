package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for rejection reason validation in ModerationService.
 *
 * Property 4: Rejection requires a non-empty reason.
 * Generate random strings (including null, empty, whitespace-only).
 * Verify rejection fails for invalid reasons, succeeds for valid non-empty strings.
 *
 * Validates: Requirements REQ-MOD-03
 */
@Tag("Feature: admin-panel, Property 4: Rejection requires a non-empty reason")
class RejectionReasonPropertyTest {

    private ModerationService moderationService;
    private ListingRepository listingRepository;
    private AuditService auditService;

    private final User moderator = User.builder()
            .id(1L)
            .phone("9000000001")
            .password("password123")
            .name("Moderator")
            .role("CHECKER")
            .banned(false)
            .build();

    @BeforeProperty
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        auditService = mock(AuditService.class);
        NotificationService notificationService = mock(NotificationService.class);
        moderationService = new ModerationService(listingRepository, auditService, notificationService);

        Listing pendingListing = Listing.builder()
                .id(1L)
                .title("Test Listing")
                .category("agricultural-products")
                .status("PENDING")
                .user(moderator)
                .build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(pendingListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Property: Rejection with an invalid reason (null, empty, or whitespace-only)
     * must always throw IllegalArgumentException.
     *
     * Validates: Requirements REQ-MOD-03
     */
    @Property(tries = 100)
    void rejectListing_throwsException_forInvalidReasons(
            @ForAll("invalidReasons") String reason
    ) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> moderationService.rejectListing(1L, reason, moderator),
                "Expected IllegalArgumentException for invalid reason: '" + reason + "'");
        assertEquals("Rejection reason is required", ex.getMessage());
    }

    /**
     * Property: Rejection with a null reason must throw IllegalArgumentException.
     *
     * Validates: Requirements REQ-MOD-03
     */
    @Property(tries = 10)
    void rejectListing_throwsException_forNullReason() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> moderationService.rejectListing(1L, null, moderator),
                "Expected IllegalArgumentException for null reason");
        assertEquals("Rejection reason is required", ex.getMessage());
    }

    /**
     * Property: Rejection with a valid non-empty, non-blank reason must succeed
     * (no exception thrown).
     *
     * Validates: Requirements REQ-MOD-03
     */
    @Property(tries = 100)
    void rejectListing_succeeds_forValidReasons(
            @ForAll("validReasons") String reason
    ) {
        // Reset mock state for each trial (fresh PENDING listing)
        Listing pendingListing = Listing.builder()
                .id(1L)
                .title("Test Listing")
                .category("agricultural-products")
                .status("PENDING")
                .user(moderator)
                .build();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(pendingListing));

        assertDoesNotThrow(
                () -> moderationService.rejectListing(1L, reason, moderator),
                "Expected rejection to succeed for valid reason: '" + reason + "'");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<String> invalidReasons() {
        return Arbitraries.of(
                "",           // empty string
                " ",          // single space
                "   ",        // multiple spaces
                "\t",         // tab
                "\n",         // newline
                "  \t  ",    // mixed whitespace
                "\t\n\t",    // tabs and newlines
                "    \n   "  // spaces and newlines
        );
    }

    @Provide
    Arbitrary<String> validReasons() {
        return Arbitraries.oneOf(
                // Realistic rejection reasons
                Arbitraries.of(
                        "Spam",
                        "Duplicate listing",
                        "Inappropriate content",
                        "Misleading information",
                        "Prohibited item",
                        "Low quality images"
                ),
                // Random non-blank strings
                Arbitraries.strings()
                        .ofMinLength(1)
                        .ofMaxLength(200)
                        .filter(s -> !s.isBlank())
        );
    }
}
