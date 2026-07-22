package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final AuditService auditService;

    /**
     * Returns banners that are active AND whose current date falls within their
     * start/end date range. Banners without startDate or endDate are considered
     * always valid (if active=true).
     */
    public List<Banner> getActiveBanners() {
        List<Banner> activeBanners = bannerRepository.findByActiveTrueOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();

        return activeBanners.stream()
                .filter(banner -> isWithinDateRange(banner, now))
                .collect(Collectors.toList());
    }

    public List<Banner> getAllBanners() {
        return bannerRepository.findAll(Sort.by("createdAt").descending());
    }

    @Transactional
    public Banner createBanner(Banner banner, User actor) {
        banner.setCreatedBy(actor);
        Banner saved = bannerRepository.save(banner);
        auditService.audit(actor, "CREATE_BANNER", "BANNER", saved.getId(), banner.getTitle());
        return saved;
    }

    @Transactional
    public Banner updateBanner(Long bannerId, Banner updatedFields, User actor) {
        Banner existing = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new RuntimeException("Banner not found"));

        if (updatedFields.getTitle() != null) {
            existing.setTitle(updatedFields.getTitle());
        }
        if (updatedFields.getSubtitle() != null) {
            existing.setSubtitle(updatedFields.getSubtitle());
        }
        if (updatedFields.getImageUrl() != null) {
            existing.setImageUrl(updatedFields.getImageUrl());
        }
        if (updatedFields.getLink() != null) {
            existing.setLink(updatedFields.getLink());
        }
        if (updatedFields.getColor() != null) {
            existing.setColor(updatedFields.getColor());
        }
        if (updatedFields.getActive() != null) {
            existing.setActive(updatedFields.getActive());
        }
        // startDate and endDate can be explicitly set to null to remove them
        existing.setStartDate(updatedFields.getStartDate());
        existing.setEndDate(updatedFields.getEndDate());

        Banner saved = bannerRepository.save(existing);
        auditService.audit(actor, "UPDATE_BANNER", "BANNER", bannerId, existing.getTitle());
        return saved;
    }

    @Transactional
    public void deleteBanner(Long bannerId, User actor) {
        bannerRepository.deleteById(bannerId);
        auditService.audit(actor, "DELETE_BANNER", "BANNER", bannerId, null);
    }

    /**
     * Checks if the current time falls within the banner's date range.
     * - If startDate is null and endDate is null, the banner is always valid.
     * - If only startDate is set, the banner is valid from startDate onward.
     * - If only endDate is set, the banner is valid until endDate.
     * - If both are set, the banner is valid between startDate and endDate (inclusive).
     */
    public boolean isWithinDateRange(Banner banner, LocalDateTime now) {
        LocalDateTime start = banner.getStartDate();
        LocalDateTime end = banner.getEndDate();

        if (start != null && now.isBefore(start)) {
            return false;
        }
        if (end != null && now.isAfter(end)) {
            return false;
        }
        return true;
    }
}
