package com.dealspot.service;

import com.dealspot.dto.ListingResponse;
import com.dealspot.dto.UserModerationOverview;
import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ListingRepository listingRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    /**
     * Valid status transitions for the listing state machine.
     * PENDING -> ACTIVE (approve), REJECTED (reject), FLAGGED (flag)
     * FLAGGED -> ACTIVE (admin approves), REJECTED (admin rejects)
     * ACTIVE  -> PENDING (re-submit after edit), EXPIRED (TTL expires)
     */
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "PENDING", Set.of("ACTIVE", "REJECTED", "FLAGGED"),
            "FLAGGED", Set.of("ACTIVE", "REJECTED"),
            // ACTIVE listings can be taken down (REJECTED) or flagged by a moderator.
            "ACTIVE", Set.of("PENDING", "EXPIRED", "REJECTED", "FLAGGED")
    );

    /**
     * Validates and applies a status transition on a listing.
     * Throws IllegalStateException if the transition is not allowed.
     */
    public void transitionStatus(Listing listing, String newStatus) {
        Set<String> allowed = VALID_TRANSITIONS.getOrDefault(listing.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + listing.getStatus() + " to " + newStatus);
        }
        listing.setStatus(newStatus);
    }

    public Page<Listing> getModerationQueue(int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").ascending());
        return listingRepository.findByStatus("PENDING", pageable);
    }

    @Transactional
    public void approveListing(Long listingId, User moderator) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        transitionStatus(listing, "ACTIVE");
        listing.setModeratedBy(moderator);
        listing.setModeratedAt(LocalDateTime.now());
        listingRepository.save(listing);

        auditService.audit(moderator, "APPROVE_LISTING", "LISTING", listingId, null);

        notificationService.create(
                listing.getUser(),
                "Listing Approved",
                "Your listing '" + listing.getTitle() + "' has been approved and is now live!",
                "MODERATION",
                "LISTING",
                listingId
        );
    }

    @Transactional
    public void rejectListing(Long listingId, String reason, User moderator) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        transitionStatus(listing, "REJECTED");
        listing.setRejectionReason(reason);
        listing.setModeratedBy(moderator);
        listing.setModeratedAt(LocalDateTime.now());
        listingRepository.save(listing);

        auditService.audit(moderator, "REJECT_LISTING", "LISTING", listingId, reason);

        notificationService.create(
                listing.getUser(),
                "Listing Rejected",
                "Your listing '" + listing.getTitle() + "' was rejected. Reason: " + reason,
                "MODERATION",
                "LISTING",
                listingId
        );
    }

    @Transactional
    public void flagListing(Long listingId, User moderator) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        transitionStatus(listing, "FLAGGED");
        listing.setModeratedBy(moderator);
        listing.setModeratedAt(LocalDateTime.now());
        listingRepository.save(listing);

        auditService.audit(moderator, "FLAG_LISTING", "LISTING", listingId, null);
    }

    /**
     * Takes down a listing — pulls it out of public view by moving it to REJECTED.
     * Works for ACTIVE (live), FLAGGED or PENDING listings. A reason is required and
     * the seller is notified. Used by admins/checkers to remove bad live ads.
     */
    @Transactional
    public void takeDownListing(Long listingId, String reason, User moderator) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A takedown reason is required");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Already removed — nothing to do.
        if ("REJECTED".equals(listing.getStatus())) {
            return;
        }

        transitionStatus(listing, "REJECTED");
        listing.setRejectionReason(reason);
        listing.setModeratedBy(moderator);
        listing.setModeratedAt(LocalDateTime.now());
        listingRepository.save(listing);

        auditService.audit(moderator, "TAKEDOWN_LISTING", "LISTING", listingId, reason);

        notificationService.create(
                listing.getUser(),
                "Listing Removed",
                "Your listing '" + listing.getTitle() + "' was removed by our team. Reason: " + reason,
                "MODERATION",
                "LISTING",
                listingId
        );
    }

    /**
     * Builds the moderator overview for the owner of a given listing: their
     * account + ban status, the ads of theirs that have been reported, and all
     * of their posted ads (any status). Used when a moderator opens a report.
     */
    public UserModerationOverview getOwnerOverviewForListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        User owner = listing.getUser();

        // All of this owner's listings (unpaged — a single seller's catalogue is small).
        List<Listing> all = listingRepository
                .findByUserId(owner.getId(), Pageable.unpaged())
                .getContent();

        int activeCount = (int) all.stream().filter(l -> "ACTIVE".equals(l.getStatus())).count();

        List<ListingResponse> allResponses = all.stream()
                .map(ListingResponse::fromEntity)
                .toList();

        // Reports against this owner's listings, grouped per listing.
        List<Long> ownerListingIds = all.stream().map(Listing::getId).toList();
        Map<Long, List<Report>> reportsByListing = new LinkedHashMap<>();
        if (!ownerListingIds.isEmpty()) {
            for (Report r : reportRepository
                    .findByTargetTypeAndTargetIdInOrderByCreatedAtDesc("LISTING", ownerListingIds)) {
                reportsByListing.computeIfAbsent(r.getTargetId(), k -> new ArrayList<>()).add(r);
            }
        }

        List<UserModerationOverview.ReportedListing> reported = new ArrayList<>();
        for (Listing l : all) {
            List<Report> reports = reportsByListing.get(l.getId());
            if (reports != null && !reports.isEmpty()) {
                reported.add(UserModerationOverview.ReportedListing.builder()
                        .listing(ListingResponse.fromEntity(l))
                        .reportCount(reports.size())
                        .reasons(reports.stream().map(Report::getReason).distinct().toList())
                        .build());
            }
        }

        UserModerationOverview.OwnerInfo ownerInfo = UserModerationOverview.OwnerInfo.builder()
                .id(owner.getId())
                .name(owner.getName())
                .phone(owner.getPhone())
                .email(owner.getEmail())
                .location(owner.getLocation())
                .district(owner.getDistrict())
                .role(owner.getRole())
                .banned(owner.getBanned())
                .banReason(owner.getBanReason())
                .createdAt(owner.getCreatedAt())
                .totalListings(all.size())
                .activeListings(activeCount)
                .build();

        return UserModerationOverview.builder()
                .owner(ownerInfo)
                .reportedListings(reported)
                .allListings(allResponses)
                .build();
    }

    @Transactional
    public void featureListing(Long listingId, boolean featured, User actor) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setFeatured(featured);
        listing.setPromoted(featured); // REQ-ADS-04: featured listings get "promoted" badge
        listingRepository.save(listing);

        auditService.audit(actor, featured ? "FEATURE_LISTING" : "UNFEATURE_LISTING", "LISTING", listingId, null);
    }

    /**
     * Returns moderation stats: pending count, approved today, rejected today.
     */
    public Map<String, Object> getModerationStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long pendingCount = listingRepository.countByStatus("PENDING");
        long approvedToday = listingRepository.countByStatusAndModeratedAtGreaterThanEqual("ACTIVE", startOfDay);
        long rejectedToday = listingRepository.countByStatusAndModeratedAtGreaterThanEqual("REJECTED", startOfDay);

        return Map.of(
                "pendingCount", pendingCount,
                "approvedToday", approvedToday,
                "rejectedToday", rejectedToday
        );
    }
}
