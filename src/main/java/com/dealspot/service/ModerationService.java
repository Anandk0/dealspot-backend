package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ListingRepository listingRepository;
    private final AuditLogRepository auditLogRepository;

    public Page<Listing> getModerationQueue(int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").ascending());
        return listingRepository.findByStatus("PENDING", pageable);
    }

    @Transactional
    public void approveListing(Long listingId, User moderator) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setStatus("ACTIVE");
        listing.setModeratedBy(moderator);
        listing.setModeratedAt(LocalDateTime.now());
        listingRepository.save(listing);

        audit(moderator, "APPROVE_LISTING", "LISTING", listingId, null);
    }

    @Transactional
    public void rejectListing(Long listingId, String reason, User moderator) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setStatus("REJECTED");
        listing.setRejectionReason(reason);
        listing.setModeratedBy(moderator);
        listing.setModeratedAt(LocalDateTime.now());
        listingRepository.save(listing);

        audit(moderator, "REJECT_LISTING", "LISTING", listingId, reason);
    }

    @Transactional
    public void flagListing(Long listingId, User moderator) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setStatus("FLAGGED");
        listing.setModeratedBy(moderator);
        listing.setModeratedAt(LocalDateTime.now());
        listingRepository.save(listing);

        audit(moderator, "FLAG_LISTING", "LISTING", listingId, null);
    }

    @Transactional
    public void featureListing(Long listingId, boolean featured, User actor) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setFeatured(featured);
        listingRepository.save(listing);

        audit(actor, featured ? "FEATURE_LISTING" : "UNFEATURE_LISTING", "LISTING", listingId, null);
    }

    public void audit(User actor, String action, String targetType, Long targetId, String details) {
        AuditLog log = AuditLog.builder()
                .actorId(actor.getId())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}
