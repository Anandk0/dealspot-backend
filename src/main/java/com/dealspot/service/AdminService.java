package com.dealspot.service;

import com.dealspot.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserManagementService userManagementService;
    private final ModerationService moderationService;
    private final StatsService statsService;
    private final BannerService bannerService;
    private final SettingsService settingsService;
    private final AuditService auditService;

    // ─── Role Management ──────────────────────────────────
    public void checkRole(User user, String... allowedRoles) {
        userManagementService.checkRole(user, allowedRoles);
    }

    public void changeUserRole(Long targetUserId, String newRole, User actor) {
        userManagementService.changeUserRole(targetUserId, newRole, actor);
    }

    // ─── User Management ──────────────────────────────────
    public Page<User> getAllUsers(int page, int size, String search) {
        return userManagementService.getAllUsers(page, size, search);
    }

    public void banUser(Long userId, String reason, User actor) {
        userManagementService.banUser(userId, reason, actor);
    }

    public void unbanUser(Long userId, User actor) {
        userManagementService.unbanUser(userId, actor);
    }

    // ─── Moderation ───────────────────────────────────────
    public Page<Listing> getModerationQueue(int page, int size) {
        return moderationService.getModerationQueue(page, size);
    }

    public void approveListing(Long listingId, User moderator) {
        moderationService.approveListing(listingId, moderator);
    }

    public void rejectListing(Long listingId, String reason, User moderator) {
        moderationService.rejectListing(listingId, reason, moderator);
    }

    public void flagListing(Long listingId, User moderator) {
        moderationService.flagListing(listingId, moderator);
    }

    public void featureListing(Long listingId, boolean featured, User actor) {
        moderationService.featureListing(listingId, featured, actor);
    }

    // ─── Dashboard Stats ──────────────────────────────────
    public Map<String, Object> getDashboardStats() {
        return statsService.getDashboardStats();
    }

    public Map<String, Object> getRevenueStats(LocalDate from, LocalDate to) {
        return statsService.getRevenueStats(from, to);
    }

    // ─── Banners ──────────────────────────────────────────
    public List<Banner> getActiveBanners() {
        return bannerService.getActiveBanners();
    }

    public List<Banner> getAllBanners() {
        return bannerService.getAllBanners();
    }

    public Banner createBanner(Banner banner, User actor) {
        return bannerService.createBanner(banner, actor);
    }

    public void deleteBanner(Long bannerId, User actor) {
        bannerService.deleteBanner(bannerId, actor);
    }

    // ─── Settings ─────────────────────────────────────────
    public Map<String, String> getAllSettings() {
        return settingsService.getAllSettings();
    }

    public void updateSetting(String key, String value, User actor) {
        settingsService.updateSetting(key, value, actor);
    }

    // ─── Audit ────────────────────────────────────────────
    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditService.getAuditLogs(page, size);
    }
}
