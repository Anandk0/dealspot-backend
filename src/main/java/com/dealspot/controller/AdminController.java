package com.dealspot.controller;

import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.*;
import com.dealspot.repository.PaymentOrderRepository;
import com.dealspot.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PaymentOrderRepository paymentOrderRepository;

    // ─── Dashboard ────────────────────────────────────────
    @GetMapping("/stats/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(@AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<Map<String, Object>> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getRevenueStats(from, to));
    }

    @GetMapping("/stats/transactions")
    public ResponseEntity<Page<PaymentOrder>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(paymentOrderRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)));
    }

    // ─── Moderation ───────────────────────────────────────
    @GetMapping("/moderation/queue")
    public ResponseEntity<Page<ListingResponse>> moderationQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "CHECKER", "ADMIN", "SUPER_ADMIN");
        Page<ListingResponse> listings = adminService.getModerationQueue(page, size)
                .map(ListingResponse::fromEntity);
        return ResponseEntity.ok(listings);
    }

    @PutMapping("/moderation/{listingId}/approve")
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        adminService.approveListing(listingId, user);
        return ResponseEntity.ok(Map.of("message", "Listing approved"));
    }

    @PutMapping("/moderation/{listingId}/reject")
    public ResponseEntity<Map<String, String>> reject(
            @PathVariable Long listingId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        adminService.rejectListing(listingId, body.get("reason"), user);
        return ResponseEntity.ok(Map.of("message", "Listing rejected"));
    }

    @PutMapping("/moderation/{listingId}/flag")
    public ResponseEntity<Map<String, String>> flag(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        adminService.flagListing(listingId, user);
        return ResponseEntity.ok(Map.of("message", "Listing flagged"));
    }

    @PutMapping("/listings/{listingId}/feature")
    public ResponseEntity<Map<String, String>> feature(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        adminService.featureListing(listingId, true, user);
        return ResponseEntity.ok(Map.of("message", "Listing featured"));
    }

    @PutMapping("/listings/{listingId}/unfeature")
    public ResponseEntity<Map<String, String>> unfeature(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        adminService.featureListing(listingId, false, user);
        return ResponseEntity.ok(Map.of("message", "Listing unfeatured"));
    }

    // ─── User Management ──────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<Page<User>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getAllUsers(page, size, search));
    }

    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<Map<String, String>> ban(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        adminService.banUser(userId, body.get("reason"), user);
        return ResponseEntity.ok(Map.of("message", "User banned"));
    }

    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<Map<String, String>> unban(
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        adminService.unbanUser(userId, user);
        return ResponseEntity.ok(Map.of("message", "User unbanned"));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, String>> changeRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        adminService.changeUserRole(userId, body.get("role"), user);
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    // ─── Banners ──────────────────────────────────────────
    @GetMapping("/banners")
    public ResponseEntity<List<Banner>> banners(@AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getAllBanners());
    }

    @PostMapping("/banners")
    public ResponseEntity<Banner> createBanner(
            @RequestBody Banner banner,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(adminService.createBanner(banner, user));
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<Map<String, String>> deleteBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        adminService.deleteBanner(id, user);
        return ResponseEntity.ok(Map.of("message", "Banner deleted"));
    }

    // ─── Settings ─────────────────────────────────────────
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getSettings(@AuthenticationPrincipal User user) {
        adminService.checkRole(user, "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getAllSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<Map<String, String>> updateSettings(
            @RequestBody Map<String, String> settings,
            @AuthenticationPrincipal User user) {
        settings.forEach((key, value) -> adminService.updateSetting(key, value, user));
        return ResponseEntity.ok(Map.of("message", "Settings updated"));
    }

    // ─── Audit Logs ───────────────────────────────────────
    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLog>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getAuditLogs(page, size));
    }
}
