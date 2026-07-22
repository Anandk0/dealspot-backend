package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks private ModerationService moderationService;

    private User moderator;
    private Listing pendingListing;
    private Listing flaggedListing;
    private Listing activeListing;

    @BeforeEach
    void setUp() {
        moderator = User.builder().id(1L).phone("9000000001").name("Checker").role("CHECKER").banned(false).build();

        pendingListing = Listing.builder().id(1L).title("Test Listing").category("livestock").status("PENDING").user(moderator).build();
        flaggedListing = Listing.builder().id(2L).title("Flagged Listing").category("livestock").status("FLAGGED").user(moderator).build();
        activeListing = Listing.builder().id(3L).title("Active Listing").category("livestock").status("ACTIVE").user(moderator).build();
    }

    // ─── transitionStatus valid transitions ──────────────────────

    @Test
    void transitionStatus_pending_to_active_shouldSucceed() {
        moderationService.transitionStatus(pendingListing, "ACTIVE");
        assertEquals("ACTIVE", pendingListing.getStatus());
    }

    @Test
    void transitionStatus_pending_to_rejected_shouldSucceed() {
        moderationService.transitionStatus(pendingListing, "REJECTED");
        assertEquals("REJECTED", pendingListing.getStatus());
    }

    @Test
    void transitionStatus_pending_to_flagged_shouldSucceed() {
        moderationService.transitionStatus(pendingListing, "FLAGGED");
        assertEquals("FLAGGED", pendingListing.getStatus());
    }

    @Test
    void transitionStatus_flagged_to_active_shouldSucceed() {
        moderationService.transitionStatus(flaggedListing, "ACTIVE");
        assertEquals("ACTIVE", flaggedListing.getStatus());
    }

    @Test
    void transitionStatus_flagged_to_rejected_shouldSucceed() {
        moderationService.transitionStatus(flaggedListing, "REJECTED");
        assertEquals("REJECTED", flaggedListing.getStatus());
    }

    @Test
    void transitionStatus_active_to_pending_shouldSucceed() {
        moderationService.transitionStatus(activeListing, "PENDING");
        assertEquals("PENDING", activeListing.getStatus());
    }

    @Test
    void transitionStatus_active_to_expired_shouldSucceed() {
        moderationService.transitionStatus(activeListing, "EXPIRED");
        assertEquals("EXPIRED", activeListing.getStatus());
    }

    // ─── transitionStatus invalid transitions ──────────────────────

    @Test
    void transitionStatus_pending_to_expired_shouldThrow() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> moderationService.transitionStatus(pendingListing, "EXPIRED"));
        assertEquals("Cannot transition from PENDING to EXPIRED", ex.getMessage());
    }

    @Test
    void transitionStatus_active_to_rejected_shouldThrow() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> moderationService.transitionStatus(activeListing, "REJECTED"));
        assertEquals("Cannot transition from ACTIVE to REJECTED", ex.getMessage());
    }

    @Test
    void transitionStatus_flagged_to_pending_shouldThrow() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> moderationService.transitionStatus(flaggedListing, "PENDING"));
        assertEquals("Cannot transition from FLAGGED to PENDING", ex.getMessage());
    }

    @Test
    void transitionStatus_rejected_to_active_shouldThrow() {
        Listing rejectedListing = Listing.builder().id(4L).title("Rejected").category("livestock").status("REJECTED").user(moderator).build();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> moderationService.transitionStatus(rejectedListing, "ACTIVE"));
        assertEquals("Cannot transition from REJECTED to ACTIVE", ex.getMessage());
    }

    // ─── approveListing ──────────────────────────────────────────

    @Test
    void approveListing_shouldTransitionPendingToActive() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(pendingListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(pendingListing);

        moderationService.approveListing(1L, moderator);

        assertEquals("ACTIVE", pendingListing.getStatus());
        assertEquals(moderator, pendingListing.getModeratedBy());
        assertNotNull(pendingListing.getModeratedAt());
        verify(auditService).audit(moderator, "APPROVE_LISTING", "LISTING", 1L, null);
    }

    @Test
    void approveListing_shouldFailForActiveListing() {
        when(listingRepository.findById(3L)).thenReturn(Optional.of(activeListing));

        assertThrows(IllegalStateException.class,
                () -> moderationService.approveListing(3L, moderator));
    }

    // ─── rejectListing ───────────────────────────────────────────

    @Test
    void rejectListing_shouldTransitionPendingToRejected() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(pendingListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(pendingListing);

        moderationService.rejectListing(1L, "Low quality", moderator);

        assertEquals("REJECTED", pendingListing.getStatus());
        assertEquals("Low quality", pendingListing.getRejectionReason());
        assertEquals(moderator, pendingListing.getModeratedBy());
        assertNotNull(pendingListing.getModeratedAt());
        verify(auditService).audit(moderator, "REJECT_LISTING", "LISTING", 1L, "Low quality");
    }

    @Test
    void rejectListing_shouldThrow_whenReasonIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> moderationService.rejectListing(1L, null, moderator));
        assertEquals("Rejection reason is required", ex.getMessage());
    }

    @Test
    void rejectListing_shouldThrow_whenReasonIsEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> moderationService.rejectListing(1L, "", moderator));
        assertEquals("Rejection reason is required", ex.getMessage());
    }

    @Test
    void rejectListing_shouldThrow_whenReasonIsWhitespaceOnly() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> moderationService.rejectListing(1L, "   ", moderator));
        assertEquals("Rejection reason is required", ex.getMessage());
    }

    // ─── flagListing ─────────────────────────────────────────────

    @Test
    void flagListing_shouldTransitionPendingToFlagged() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(pendingListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(pendingListing);

        moderationService.flagListing(1L, moderator);

        assertEquals("FLAGGED", pendingListing.getStatus());
        assertEquals(moderator, pendingListing.getModeratedBy());
        assertNotNull(pendingListing.getModeratedAt());
        verify(auditService).audit(moderator, "FLAG_LISTING", "LISTING", 1L, null);
    }

    @Test
    void flagListing_shouldFailForActiveListing() {
        when(listingRepository.findById(3L)).thenReturn(Optional.of(activeListing));

        assertThrows(IllegalStateException.class,
                () -> moderationService.flagListing(3L, moderator));
    }
}
