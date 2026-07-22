package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.repository.ListingRepository;
import net.jqwik.api.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Property-based test for listing status transitions in ModerationService.
 *
 * Validates: Requirements REQ-MOD-01, REQ-MOD-02
 */
@Tag("admin-panel")
@Tag("listing-status-transitions")
class ListingStatusTransitionPropertyTest {

    private static final List<String> ALL_STATUSES = List.of(
            "PENDING", "ACTIVE", "REJECTED", "FLAGGED", "SOLD", "EXPIRED"
    );

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "PENDING", Set.of("ACTIVE", "REJECTED", "FLAGGED"),
            "FLAGGED", Set.of("ACTIVE", "REJECTED"),
            "ACTIVE", Set.of("PENDING", "EXPIRED")
    );

    private final ModerationService moderationService;

    ListingStatusTransitionPropertyTest() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        AuditService auditService = mock(AuditService.class);
        NotificationService notificationService = mock(NotificationService.class);
        this.moderationService = new ModerationService(listingRepository, auditService, notificationService);
    }

    /**
     * Property 3: Listing status transitions follow the state machine.
     *
     * For any (currentStatus, attemptedNewStatus) pair from the full status space,
     * transitionStatus succeeds if and only if the transition is defined in VALID_TRANSITIONS.
     *
     * Validates: Requirements REQ-MOD-01, REQ-MOD-02
     */
    @Property(tries = 200)
    void transitionStatus_succeedsOnlyForValidTransitions(
            @ForAll("allStatuses") String currentStatus,
            @ForAll("allStatuses") String attemptedNewStatus
    ) {
        Listing listing = Listing.builder()
                .id(1L)
                .title("Test Listing")
                .category("services")
                .status(currentStatus)
                .build();

        Set<String> allowedTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        boolean shouldSucceed = allowedTargets.contains(attemptedNewStatus);

        if (shouldSucceed) {
            // Valid transition — should not throw and should update status
            assertDoesNotThrow(() -> moderationService.transitionStatus(listing, attemptedNewStatus),
                    "Expected transition from '" + currentStatus + "' to '" + attemptedNewStatus + "' to succeed");
            assertEquals(attemptedNewStatus, listing.getStatus(),
                    "Status should be updated to '" + attemptedNewStatus + "' after valid transition");
        } else {
            // Invalid transition — should throw IllegalStateException
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> moderationService.transitionStatus(listing, attemptedNewStatus),
                    "Expected transition from '" + currentStatus + "' to '" + attemptedNewStatus + "' to be rejected");
            assertTrue(ex.getMessage().contains("Cannot transition from"),
                    "Exception message should indicate invalid transition");
        }
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<String> allStatuses() {
        return Arbitraries.of(ALL_STATUSES);
    }
}
