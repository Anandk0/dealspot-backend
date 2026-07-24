package com.dealspot.controller;

import com.dealspot.dto.*;
import com.dealspot.entity.*;
import com.dealspot.service.AdminService;
import com.dealspot.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final CloudinaryService cloudinaryService;

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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getTransactionHistory(page, size, from, to));
    }

    @GetMapping("/stats/revenue/export")
    public ResponseEntity<byte[]> exportRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        String csv = adminService.exportRevenueCsv(from, to);
        byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"revenue_export.csv\"")
                .body(csvBytes);
    }

    @GetMapping("/stats/users")
    public ResponseEntity<Map<String, Object>> userGrowthStats(@AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getUserGrowthStats());
    }

    @GetMapping("/stats/listings")
    public ResponseEntity<Map<String, Object>> listingStats(@AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getListingStats());
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

    @GetMapping("/moderation/stats")
    public ResponseEntity<Map<String, Object>> moderationStats(@AuthenticationPrincipal User user) {
        adminService.checkRole(user, "CHECKER", "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getModerationStats());
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
    public ResponseEntity<Page<UserResponse>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getAllUsers(page, size, search)
                .map(UserResponse::fromEntity));
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
    public ResponseEntity<List<BannerResponse>> banners(@AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getAllBanners().stream()
                .map(BannerResponse::fromEntity)
                .toList());
    }

    @PostMapping("/banners")
    public ResponseEntity<BannerResponse> createBanner(
            @Valid @RequestBody CreateBannerRequest request,
            @AuthenticationPrincipal User user) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .imageUrl(request.getImageUrl())
                .link(request.getLink())
                .color(request.getColor())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return ResponseEntity.ok(BannerResponse.fromEntity(adminService.createBanner(banner, user)));
    }

    @PostMapping("/banners/upload")
    public ResponseEntity<BannerResponse> createBannerWithImage(
            @RequestParam("title") String title,
            @RequestParam(value = "subtitle", required = false) String subtitle,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "color", required = false) String color,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadImageUrl(image);
        }

        java.time.LocalDateTime startDateTime = (startDate != null && !startDate.isBlank())
                ? java.time.LocalDate.parse(startDate).atStartOfDay() : null;
        java.time.LocalDateTime endDateTime = (endDate != null && !endDate.isBlank())
                ? java.time.LocalDate.parse(endDate).atTime(23, 59, 59) : null;

        Banner banner = Banner.builder()
                .title(title)
                .subtitle(subtitle)
                .imageUrl(imageUrl)
                .link(link)
                .color(color)
                .startDate(startDateTime)
                .endDate(endDateTime)
                .build();
        return ResponseEntity.ok(BannerResponse.fromEntity(adminService.createBanner(banner, user)));
    }

    @PutMapping("/banners/{id}")
    public ResponseEntity<BannerResponse> updateBanner(
            @PathVariable Long id,
            @RequestBody UpdateBannerRequest request,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        Banner updatedFields = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .imageUrl(request.getImageUrl())
                .link(request.getLink())
                .color(request.getColor())
                .active(request.getActive())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return ResponseEntity.ok(BannerResponse.fromEntity(adminService.updateBanner(id, updatedFields, user)));
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
        adminService.checkRole(user, "SUPER_ADMIN");
        settings.forEach((key, value) -> adminService.updateSetting(key, value, user));
        return ResponseEntity.ok(Map.of("message", "Settings updated"));
    }

    // ─── Platform Listings ────────────────────────────────
    @PostMapping("/listings/platform")
    public ResponseEntity<ListingResponse> createPlatformListing(
            @Valid @RequestBody CreatePlatformListingRequest request,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        return ResponseEntity.ok(ListingResponse.fromEntity(
                adminService.createPlatformListing(
                        request.getTitle(),
                        request.getDescription(),
                        request.getCategory(),
                        request.getPrice(),
                        user)));
    }

    // ─── Audit Logs ───────────────────────────────────────
    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLog>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "SUPER_ADMIN");
        return ResponseEntity.ok(adminService.getAuditLogs(page, size, action, from, to));
    }
}
