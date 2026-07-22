package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the full moderation workflow.
 * Validates: REQ-MOD-01, REQ-MOD-02, REQ-MOD-03, REQ-MOD-04, REQ-MOD-05
 *
 * Tests complete lifecycle flows:
 * - PENDING → ACTIVE (approve with notification)
 * - PENDING → REJECTED (reject with reason and notification)
 * - PENDING → FLAGGED → ACTIVE (flag then admin approves with notification)
 */
@ExtendWith(MockitoExtension.class)
class ModerationWorkflowIntegrationTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    private ModerationService moderationService;

    private User seller;
    private User moderator;
    private User admin;

    @BeforeEach
    void setUp() {
        moderationService = new ModerationService(listingRepository, auditService, notificationService);

        seller = User.builder()
                .id(10L)
                .phone("8888888888")
                .name("Farmer")
                .role("USER")
                .banned(false)
                .build();

        moderator = User.builder()
                .id(2L)
                .phone("9000000001")
                .name("Moderator")
                .role("CHECKER")
                .banned(false)
                .build();

        admin = User.builder()
                .id(1L)
                .phone("9000000000")
                .name("Admin")
                .role("ADMIN")
                .banned(false)
                .build();
    }

    // ─── Test 1: PENDING → approve → ACTIVE (notification sent) ─────────

    @Test
    @DisplayName("Full approve workflow: PENDING → ACTIVE with notification to seller")
    void approveWorkflow_pendingToActive_notifiesSeller() {
        // Arrange: create a listing in PENDING status owned by the seller
        Listing listing = Listing.builder()
                .id(1L)
                .title("Fresh Tomatoes")
                .status("PENDING")
                .category("agricultural-products")
                .user(seller)
                .build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // Act: moderator approves the listing
        moderationService.approveListing(1L, moderator);

        // Assert: status transitioned to ACTIVE
        assertEquals("ACTIVE", listing.getStatus());
        assertEquals(moderator, listing.getModeratedBy());
        assertNotNull(listing.getModeratedAt());

        // Assert: audit log recorded
        verify(auditService).audit(moderator, "APPROVE_LISTING", "LISTING", 1L, null);

        // Assert: notification sent to the seller (listing owner)
        verify(notificationService).create(
                eq(seller),
                eq("Listing Approved"),
                contains("Fresh Tomatoes"),
                eq("MODERATION"),
                eq("LISTING"),
                eq(1L)
        );
    }

    // ─── Test 2: PENDING → reject with reason → REJECTED (notification sent) ─

    @Test
    @DisplayName("Full reject workflow: PENDING → REJECTED with reason and notification to seller")
    void rejectWorkflow_pendingToRejected_notifiesSellerWithReason() {
        // Arrange: create a listing in PENDING status
        Listing listing = Listing.builder()
                .id(2L)
                .title("Fresh Tomatoes")
                .status("PENDING")
                .category("agricultural-products")
                .user(seller)
                .build();

        when(listingRepository.findById(2L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        String rejectionReason = "Inappropriate content";

        // Act: moderator rejects the listing with a reason
        moderationService.rejectListing(2L, rejectionReason, moderator);

        // Assert: status transitioned to REJECTED with reason stored
        assertEquals("REJECTED", listing.getStatus());
        assertEquals("Inappropriate content", listing.getRejectionReason());
        assertEquals(moderator, listing.getModeratedBy());
        assertNotNull(listing.getModeratedAt());

        // Assert: audit log recorded with the rejection reason
        verify(auditService).audit(moderator, "REJECT_LISTING", "LISTING", 2L, "Inappropriate content");

        // Assert: notification sent to seller containing the reason
        verify(notificationService).create(
                eq(seller),
                eq("Listing Rejected"),
                argThat(msg -> msg.contains("Fresh Tomatoes") && msg.contains("Inappropriate content")),
                eq("MODERATION"),
                eq("LISTING"),
                eq(2L)
        );
    }

    // ─── Test 3: PENDING → flag → FLAGGED → admin approves → ACTIVE ─────

    @Test
    @DisplayName("Full flag-then-approve workflow: PENDING → FLAGGED → ACTIVE with notification")
    void flagThenApproveWorkflow_pendingToFlaggedToActive_notifiesSeller() {
        // Arrange: create a listing in PENDING status
        Listing listing = Listing.builder()
                .id(3L)
                .title("Fresh Tomatoes")
                .status("PENDING")
                .category("agricultural-products")
                .user(seller)
                .build();

        when(listingRepository.findById(3L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // Act Step 1: checker flags the listing
        moderationService.flagListing(3L, moderator);

        // Assert: status transitioned to FLAGGED
        assertEquals("FLAGGED", listing.getStatus());
        assertEquals(moderator, listing.getModeratedBy());
        assertNotNull(listing.getModeratedAt());
        verify(auditService).audit(moderator, "FLAG_LISTING", "LISTING", 3L, null);

        // Act Step 2: admin approves the flagged listing (FLAGGED → ACTIVE)
        moderationService.approveListing(3L, admin);

        // Assert: status transitioned to ACTIVE
        assertEquals("ACTIVE", listing.getStatus());
        assertEquals(admin, listing.getModeratedBy());

        // Assert: audit log recorded for approval
        verify(auditService).audit(admin, "APPROVE_LISTING", "LISTING", 3L, null);

        // Assert: notification sent to the seller for approval
        verify(notificationService).create(
                eq(seller),
                eq("Listing Approved"),
                contains("Fresh Tomatoes"),
                eq("MODERATION"),
                eq("LISTING"),
                eq(3L)
        );
    }
}
