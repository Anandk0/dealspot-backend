package com.dealspot.service;

import com.dealspot.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserManagementService userManagementService;
    @Mock private ModerationService moderationService;
    @Mock private StatsService statsService;
    @Mock private BannerService bannerService;
    @Mock private SettingsService settingsService;
    @Mock private AuditService auditService;

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
        doNothing().when(userManagementService).checkRole(superAdmin, "SUPER_ADMIN");
        assertDoesNotThrow(() -> adminService.checkRole(superAdmin, "SUPER_ADMIN"));
    }

    @Test
    void checkRole_shouldPassForAdmin_whenAdminAllowed() {
        doNothing().when(userManagementService).checkRole(admin, "ADMIN", "SUPER_ADMIN");
        assertDoesNotThrow(() -> adminService.checkRole(admin, "ADMIN", "SUPER_ADMIN"));
    }

    @Test
    void checkRole_shouldFail_forRegularUser() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(userManagementService).checkRole(regularUser, "ADMIN", "SUPER_ADMIN");
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> adminService.checkRole(regularUser, "ADMIN", "SUPER_ADMIN"));
        assertTrue(ex.getMessage().contains("Insufficient permissions"));
    }

    @Test
    void checkRole_shouldFail_forChecker_whenOnlyAdminAllowed() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(userManagementService).checkRole(checker, "ADMIN", "SUPER_ADMIN");
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> adminService.checkRole(checker, "ADMIN", "SUPER_ADMIN"));
        assertTrue(ex.getMessage().contains("Insufficient permissions"));
    }

    // ─── Role Change ──────────────────────────────────────
    @Test
    void changeUserRole_shouldSucceed_whenActorIsSuperAdmin() {
        doNothing().when(userManagementService).changeUserRole(4L, "CHECKER", superAdmin);

        adminService.changeUserRole(4L, "CHECKER", superAdmin);

        verify(userManagementService).changeUserRole(4L, "CHECKER", superAdmin);
    }

    @Test
    void changeUserRole_shouldFail_whenActorIsAdmin() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(userManagementService).changeUserRole(4L, "CHECKER", admin);
        assertThrows(AccessDeniedException.class,
                () -> adminService.changeUserRole(4L, "CHECKER", admin));
    }

    @Test
    void changeUserRole_shouldFail_whenActorIsUser() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(userManagementService).changeUserRole(4L, "ADMIN", regularUser);
        assertThrows(AccessDeniedException.class,
                () -> adminService.changeUserRole(4L, "ADMIN", regularUser));
    }

    // ─── Ban User ─────────────────────────────────────────
    @Test
    void banUser_shouldSucceed_forAdmin() {
        doNothing().when(userManagementService).banUser(4L, "Spam", admin);

        adminService.banUser(4L, "Spam", admin);

        verify(userManagementService).banUser(4L, "Spam", admin);
    }

    @Test
    void banUser_shouldFail_forChecker() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(userManagementService).banUser(4L, "Spam", checker);
        assertThrows(AccessDeniedException.class,
                () -> adminService.banUser(4L, "Spam", checker));
    }

    @Test
    void banUser_shouldFail_forRegularUser() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(userManagementService).banUser(4L, "Spam", regularUser);
        assertThrows(AccessDeniedException.class,
                () -> adminService.banUser(4L, "Spam", regularUser));
    }

    // ─── Unban ────────────────────────────────────────────
    @Test
    void unbanUser_shouldClearBanFields() {
        doNothing().when(userManagementService).unbanUser(4L, admin);

        adminService.unbanUser(4L, admin);

        verify(userManagementService).unbanUser(4L, admin);
    }

    // ─── Moderation ───────────────────────────────────────
    @Test
    void approveListing_shouldSetStatusActive() {
        doNothing().when(moderationService).approveListing(1L, checker);

        adminService.approveListing(1L, checker);

        verify(moderationService).approveListing(1L, checker);
    }

    @Test
    void approveListing_shouldFail_forRegularUser() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(moderationService).approveListing(1L, regularUser);
        assertThrows(AccessDeniedException.class,
                () -> adminService.approveListing(1L, regularUser));
    }

    @Test
    void rejectListing_shouldSetStatusAndReason() {
        doNothing().when(moderationService).rejectListing(1L, "Inappropriate content", admin);

        adminService.rejectListing(1L, "Inappropriate content", admin);

        verify(moderationService).rejectListing(1L, "Inappropriate content", admin);
    }

    @Test
    void flagListing_shouldSetStatusFlagged() {
        doNothing().when(moderationService).flagListing(1L, checker);

        adminService.flagListing(1L, checker);

        verify(moderationService).flagListing(1L, checker);
    }

    // ─── Feature Listing ──────────────────────────────────
    @Test
    void featureListing_shouldSucceed_forAdmin() {
        doNothing().when(moderationService).featureListing(1L, true, admin);

        adminService.featureListing(1L, true, admin);

        verify(moderationService).featureListing(1L, true, admin);
    }

    @Test
    void featureListing_shouldFail_forChecker() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(moderationService).featureListing(1L, true, checker);
        assertThrows(AccessDeniedException.class,
                () -> adminService.featureListing(1L, true, checker));
    }
}
