package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final BannerRepository bannerRepository;
    private final AuditLogRepository auditLogRepository;
    private final PlatformSettingRepository settingRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // ─── Role Management ──────────────────────────────────
    public void checkRole(User user, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (user.getRole().equals(role)) return;
        }
        throw new RuntimeException("Access denied. Required role: " + String.join(" or ", allowedRoles));
    }

    @Transactional
    public void changeUserRole(Long targetUserId, String newRole, User actor) {
        checkRole(actor, "SUPER_ADMIN");

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);

        audit(actor, "ROLE_CHANGE", "USER", targetUserId, oldRole + " → " + newRole);
    }

    // ─── User Management ──────────────────────────────────
    public Page<User> getAllUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank()) {
            return userRepository.findAll(pageable); // TODO: add search query
        }
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void banUser(Long userId, String reason, User actor) {
        checkRole(actor, "ADMIN", "SUPER_ADMIN");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        target.setBanned(true);
        target.setBanReason(reason);
        target.setBannedAt(LocalDateTime.now());
        userRepository.save(target);

        // Revoke all tokens
        refreshTokenRepository.revokeAllByUserId(userId);

        audit(actor, "BAN_USER", "USER", userId, reason);
    }

    @Transactional
    public void unbanUser(Long userId, User actor) {
        checkRole(actor, "ADMIN", "SUPER_ADMIN");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        target.setBanned(false);
        target.setBanReason(null);
        target.setBannedAt(null);
        userRepository.save(target);

        audit(actor, "UNBAN_USER", "USER", userId, null);
    }

    // ─── Moderation ───────────────────────────────────────
    public Page<Listing> getModerationQueue(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return listingRepository.findByStatus("PENDING", pageable);
    }

    @Transactional
    public void approveListing(Long listingId, User moderator) {
        checkRole(moderator, "CHECKER", "ADMIN", "SUPER_ADMIN");

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
        checkRole(moderator, "CHECKER", "ADMIN", "SUPER_ADMIN");

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
        checkRole(moderator, "CHECKER", "ADMIN", "SUPER_ADMIN");

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
        checkRole(actor, "ADMIN", "SUPER_ADMIN");

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setFeatured(featured);
        listingRepository.save(listing);

        audit(actor, featured ? "FEATURE_LISTING" : "UNFEATURE_LISTING", "LISTING", listingId, null);
    }

    // ─── Dashboard Stats ──────────────────────────────────
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalListings", listingRepository.count());
        stats.put("activeListings", listingRepository.countByStatus("ACTIVE"));
        stats.put("pendingModeration", listingRepository.countByStatus("PENDING"));

        // Revenue
        stats.put("totalRevenue", paymentOrderRepository.sumAmountByStatus("PAID"));
        stats.put("todayRevenue", paymentOrderRepository.sumAmountByStatusAndCreatedAfter("PAID", todayStart));
        stats.put("monthRevenue", paymentOrderRepository.sumAmountByStatusAndCreatedAfter("PAID", monthStart));
        stats.put("totalUnlocks", paymentOrderRepository.countByStatus("PAID"));

        return stats;
    }

    public Map<String, Object> getRevenueStats(LocalDate from, LocalDate to) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        stats.put("totalAmount", paymentOrderRepository.sumAmountByStatusAndCreatedBetween("PAID", fromDt, toDt));
        stats.put("totalTransactions", paymentOrderRepository.countByStatusAndCreatedAtBetween("PAID", fromDt, toDt));
        stats.put("failedTransactions", paymentOrderRepository.countByStatusAndCreatedAtBetween("FAILED", fromDt, toDt));

        return stats;
    }

    // ─── Banners ──────────────────────────────────────────
    public List<Banner> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Banner> getAllBanners() {
        return bannerRepository.findAll(Sort.by("createdAt").descending());
    }

    public Banner createBanner(Banner banner, User actor) {
        checkRole(actor, "ADMIN", "SUPER_ADMIN");
        banner.setCreatedBy(actor);
        Banner saved = bannerRepository.save(banner);
        audit(actor, "CREATE_BANNER", "BANNER", saved.getId(), banner.getTitle());
        return saved;
    }

    public void deleteBanner(Long bannerId, User actor) {
        checkRole(actor, "ADMIN", "SUPER_ADMIN");
        bannerRepository.deleteById(bannerId);
        audit(actor, "DELETE_BANNER", "BANNER", bannerId, null);
    }

    // ─── Settings ─────────────────────────────────────────
    public Map<String, String> getAllSettings() {
        Map<String, String> settings = new HashMap<>();
        settingRepository.findAll().forEach(s -> settings.put(s.getKey(), s.getValue()));
        return settings;
    }

    public void updateSetting(String key, String value, User actor) {
        checkRole(actor, "SUPER_ADMIN");
        PlatformSetting setting = settingRepository.findById(key)
                .orElse(PlatformSetting.builder().key(key).build());
        setting.setValue(value);
        setting.setUpdatedBy(actor.getId());
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(setting);
        audit(actor, "UPDATE_SETTING", "SETTING", null, key + "=" + value);
    }

    // ─── Audit ────────────────────────────────────────────
    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
    }

    private void audit(User actor, String action, String targetType, Long targetId, String details) {
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
