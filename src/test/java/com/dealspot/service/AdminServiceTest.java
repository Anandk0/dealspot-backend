package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.entity.User;
import com.dealspot.repository.*;
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
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private BannerRepository bannerRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PlatformSettingRepository settingRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private AdminService adminService;

    private User superAdmin;
    private User admin;
    private User checker;
    private User regularUser;

    @BeforeEach
    void setUp() {
        superAdmin = User.builder().id(1L).phone("9000000001").name("Super").role("SUPER_ADMIN").banned(false).build();
        admin = User.builder().id(2L).phone("9000000002").name("Admin").role("ADMIN").banned(false).build();
        checker = User.builder().id(3L).phone("9000000003").name("Checker").role("CHECKER").banned(false).build();
        regularUser = User.builder().id(4L).phone("9876543210").name("User").role("USER").banned(false).build();
    }

    // ─── Role Checks ──────────────────────────────────────
    @Test
    void checkRole_shouldPassForSuperAdmin() {
        assertDoesNotThrow(() -> adminService.checkRole(superAdmin, "SUPER_ADMIN"));
    }

    @Test
    void checkRole_shouldPassForAdmin_whenAdminAllowed() {
        assertDoesNotThrow(() -> adminService.checkRole(admin, "ADMIN", "SUPER_ADMIN"));
    }

    @Test
    void checkRole_shouldFail_forRegularUser() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.checkRole(regularUser, "ADMIN", "SUPER_ADMIN"));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    void checkRole_shouldFail_forChecker_whenOnlyAdminAllowed() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.checkRole(checker, "ADMIN", "SUPER_ADMIN"));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    // ─── Role Change ──────────────────────────────────────
    @Test
    void changeUserRole_shouldSucceed_whenActorIsSuperAdmin() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any())).thenReturn(regularUser);
        when(auditLogRepository.save(any())).thenReturn(null);

        adminService.changeUserRole(4L, "CHECKER", superAdmin);

        assertEquals("CHECKER", regularUser.getRole());
        verify(userRepository).save(regularUser);
        verify(auditLogRepository).save(any()); // Audit logged
    }

    @Test
    void changeUserRole_shouldFail_whenActorIsAdmin() {
        assertThrows(RuntimeException.class,
                () -> adminService.changeUserRole(4L, "CHECKER", admin));
    }

    @Test
    void changeUserRole_shouldFail_whenActorIsUser() {
        assertThrows(RuntimeException.class,
                () -> adminService.changeUserRole(4L, "ADMIN", regularUser));
    }

    // ─── Ban User ─────────────────────────────────────────
    @Test
    void banUser_shouldSucceed_forAdmin() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any())).thenReturn(regularUser);
        when(auditLogRepository.save(any())).thenReturn(null);

        adminService.banUser(4L, "Spam", admin);

        assertTrue(regularUser.getBanned());
        assertEquals("Spam", regularUser.getBanReason());
        assertNotNull(regularUser.getBannedAt());
        verify(refreshTokenRepository).revokeAllByUserId(4L);
    }

    @Test
    void banUser_shouldFail_forChecker() {
        assertThrows(RuntimeException.class,
                () -> adminService.banUser(4L, "Spam", checker));
    }

    @Test
    void banUser_shouldFail_forRegularUser() {
        assertThrows(RuntimeException.class,
                () -> adminService.banUser(4L, "Spam", regularUser));
    }

    // ─── Unban ────────────────────────────────────────────
    @Test
    void unbanUser_shouldClearBanFields() {
        regularUser.setBanned(true);
        regularUser.setBanReason("Test");

        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any())).thenReturn(regularUser);
        when(auditLogRepository.save(any())).thenReturn(null);

        adminService.unbanUser(4L, admin);

        assertFalse(regularUser.getBanned());
        assertNull(regularUser.getBanReason());
    }

    // ─── Moderation ───────────────────────────────────────
    @Test
    void approveListing_shouldSetStatusActive() {
        Listing listing = Listing.builder().id(1L).title("Test").status("PENDING").user(regularUser).build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenReturn(listing);
        when(auditLogRepository.save(any())).thenReturn(null);

        adminService.approveListing(1L, checker);

        assertEquals("ACTIVE", listing.getStatus());
        assertEquals(checker, listing.getModeratedBy());
        assertNotNull(listing.getModeratedAt());
    }

    @Test
    void approveListing_shouldFail_forRegularUser() {
        assertThrows(RuntimeException.class,
                () -> adminService.approveListing(1L, regularUser));
    }

    @Test
    void rejectListing_shouldSetStatusAndReason() {
        Listing listing = Listing.builder().id(1L).title("Test").status("PENDING").user(regularUser).build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenReturn(listing);
        when(auditLogRepository.save(any())).thenReturn(null);

        adminService.rejectListing(1L, "Inappropriate content", admin);

        assertEquals("REJECTED", listing.getStatus());
        assertEquals("Inappropriate content", listing.getRejectionReason());
    }

    @Test
    void flagListing_shouldSetStatusFlagged() {
        Listing listing = Listing.builder().id(1L).title("Test").status("PENDING").user(regularUser).build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenReturn(listing);
        when(auditLogRepository.save(any())).thenReturn(null);

        adminService.flagListing(1L, checker);

        assertEquals("FLAGGED", listing.getStatus());
    }

    // ─── Feature Listing ──────────────────────────────────
    @Test
    void featureListing_shouldSucceed_forAdmin() {
        Listing listing = Listing.builder().id(1L).title("Test").featured(false).user(regularUser).build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenReturn(listing);
        when(auditLogRepository.save(any())).thenReturn(null);

        adminService.featureListing(1L, true, admin);

        assertTrue(listing.getFeatured());
    }

    @Test
    void featureListing_shouldFail_forChecker() {
        assertThrows(RuntimeException.class,
                () -> adminService.featureListing(1L, true, checker));
    }
}
