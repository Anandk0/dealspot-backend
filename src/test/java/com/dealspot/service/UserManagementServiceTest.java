package com.dealspot.service;

import com.dealspot.entity.User;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuditService auditService;

    @InjectMocks private UserManagementService userManagementService;

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

    // ─── Role Hierarchy ───────────────────────────────────
    @Test
    void roleHierarchy_shouldHaveCorrectOrder() {
        List<String> hierarchy = UserManagementService.ROLE_HIERARCHY;
        assertEquals(0, hierarchy.indexOf("USER"));
        assertEquals(1, hierarchy.indexOf("CHECKER"));
        assertEquals(2, hierarchy.indexOf("ADMIN"));
        assertEquals(3, hierarchy.indexOf("SUPER_ADMIN"));
    }

    @Test
    void getRoleLevel_shouldReturnCorrectLevels() {
        assertEquals(0, UserManagementService.getRoleLevel("USER"));
        assertEquals(1, UserManagementService.getRoleLevel("CHECKER"));
        assertEquals(2, UserManagementService.getRoleLevel("ADMIN"));
        assertEquals(3, UserManagementService.getRoleLevel("SUPER_ADMIN"));
        assertEquals(-1, UserManagementService.getRoleLevel("INVALID"));
    }

    // ─── checkRole ────────────────────────────────────────
    @Test
    void checkRole_shouldAllow_whenRoleIsInAllowedSet() {
        assertDoesNotThrow(() -> userManagementService.checkRole(superAdmin, "SUPER_ADMIN"));
        assertDoesNotThrow(() -> userManagementService.checkRole(admin, "ADMIN", "SUPER_ADMIN"));
        assertDoesNotThrow(() -> userManagementService.checkRole(checker, "CHECKER", "ADMIN", "SUPER_ADMIN"));
    }

    @Test
    void checkRole_shouldDeny_whenRoleNotInAllowedSet() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> userManagementService.checkRole(regularUser, "ADMIN", "SUPER_ADMIN"));
        assertEquals("Insufficient permissions", ex.getMessage());
    }

    @Test
    void checkRole_shouldDeny_whenUserIsNull() {
        assertThrows(AccessDeniedException.class,
                () -> userManagementService.checkRole(null, "ADMIN"));
    }

    @Test
    void checkRole_shouldDeny_checkerForAdminOnly() {
        assertThrows(AccessDeniedException.class,
                () -> userManagementService.checkRole(checker, "ADMIN", "SUPER_ADMIN"));
    }

    // ─── changeUserRole ───────────────────────────────────
    @Test
    void changeUserRole_shouldSucceed_whenSuperAdminChangesOtherUserRole() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        assertDoesNotThrow(() -> userManagementService.changeUserRole(4L, "CHECKER", superAdmin));

        verify(userRepository).save(regularUser);
        verify(auditService).audit(eq(superAdmin), eq("CHANGE_ROLE"), eq("USER"), eq(4L), anyString());
    }

    @Test
    void changeUserRole_shouldFail_whenActorIsNotSuperAdmin() {
        assertThrows(AccessDeniedException.class,
                () -> userManagementService.changeUserRole(4L, "CHECKER", admin));
    }

    @Test
    void changeUserRole_shouldFail_whenTargetIsSelf() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userManagementService.changeUserRole(1L, "ADMIN", superAdmin));
        assertEquals("Cannot change own role", ex.getMessage());
    }

    @Test
    void changeUserRole_shouldFail_whenNewRoleIsSuperAdmin() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userManagementService.changeUserRole(4L, "SUPER_ADMIN", superAdmin));
        assertEquals("Invalid role: SUPER_ADMIN", ex.getMessage());
    }

    @Test
    void changeUserRole_shouldFail_whenNewRoleIsInvalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userManagementService.changeUserRole(4L, "MODERATOR", superAdmin));
        assertEquals("Invalid role: MODERATOR", ex.getMessage());
    }

    @Test
    void changeUserRole_shouldFail_whenTargetNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userManagementService.changeUserRole(999L, "ADMIN", superAdmin));
    }

    @Test
    void changeUserRole_shouldAllowDemotion_toUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenReturn(admin);

        assertDoesNotThrow(() -> userManagementService.changeUserRole(2L, "USER", superAdmin));

        verify(userRepository).save(admin);
    }

    @Test
    void changeUserRole_shouldAllowPromotion_toAdmin() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        assertDoesNotThrow(() -> userManagementService.changeUserRole(4L, "ADMIN", superAdmin));

        verify(userRepository).save(regularUser);
    }

    // ─── Ban Protection ───────────────────────────────────
    @Test
    void banUser_adminCannotBanAdmin() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                User.builder().id(2L).phone("9000000002").name("OtherAdmin").role("ADMIN").banned(false).build()));

        assertThrows(AccessDeniedException.class,
                () -> userManagementService.banUser(2L, "Violation", admin));
    }

    @Test
    void banUser_adminCannotBanSuperAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

        assertThrows(AccessDeniedException.class,
                () -> userManagementService.banUser(1L, "Violation", admin));
    }

    @Test
    void banUser_superAdminCanBanAdmin() {
        User targetAdmin = User.builder().id(5L).phone("9000000005").name("Target Admin").role("ADMIN").banned(false).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.save(any(User.class))).thenReturn(targetAdmin);

        assertDoesNotThrow(() -> userManagementService.banUser(5L, "Policy violation", superAdmin));

        assertTrue(targetAdmin.getBanned());
        assertEquals("Policy violation", targetAdmin.getBanReason());
        assertNotNull(targetAdmin.getBannedAt());
        verify(userRepository).save(targetAdmin);
    }

    @Test
    void banUser_adminCanBanRegularUser() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        assertDoesNotThrow(() -> userManagementService.banUser(4L, "Spam", admin));

        assertTrue(regularUser.getBanned());
        assertEquals("Spam", regularUser.getBanReason());
    }

    @Test
    void banUser_adminCanBanChecker() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(checker));
        when(userRepository.save(any(User.class))).thenReturn(checker);

        assertDoesNotThrow(() -> userManagementService.banUser(3L, "Misconduct", admin));

        assertTrue(checker.getBanned());
    }

    // ─── Token Revocation on Ban ──────────────────────────
    @Test
    void banUser_revokesAllRefreshTokens() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        userManagementService.banUser(4L, "Spam", admin);

        verify(refreshTokenRepository).revokeAllByUserId(4L);
    }

    @Test
    void banUser_auditsTheAction() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        userManagementService.banUser(4L, "Spam", admin);

        verify(auditService).audit(eq(admin), eq("BAN_USER"), eq("USER"), eq(4L), eq("Spam"));
    }

    // ─── Unban Restores User State ────────────────────────
    @Test
    void unbanUser_restoresState() {
        User bannedUser = User.builder().id(4L).phone("9876543210").name("Banned User").role("USER")
                .banned(true).banReason("Spam").build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(bannedUser));
        when(userRepository.save(any(User.class))).thenReturn(bannedUser);

        userManagementService.unbanUser(4L, admin);

        assertFalse(bannedUser.getBanned());
        assertNull(bannedUser.getBanReason());
        assertNull(bannedUser.getBannedAt());
        verify(userRepository).save(bannedUser);
    }

    @Test
    void unbanUser_auditsTheAction() {
        User bannedUser = User.builder().id(4L).phone("9876543210").name("Banned User").role("USER")
                .banned(true).banReason("Spam").build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(bannedUser));
        when(userRepository.save(any(User.class))).thenReturn(bannedUser);

        userManagementService.unbanUser(4L, admin);

        verify(auditService).audit(eq(admin), eq("UNBAN_USER"), eq("USER"), eq(4L), isNull());
    }

    @Test
    void unbanUser_failsForInsufficientRole() {
        assertThrows(AccessDeniedException.class,
                () -> userManagementService.unbanUser(4L, regularUser));
    }

    // ─── Search Users ─────────────────────────────────────
    @Test
    void getAllUsers_withSearch_callsSearchRepository() {
        Page<User> mockPage = new PageImpl<>(List.of(regularUser));
        when(userRepository.findByNameContainingIgnoreCaseOrPhoneContaining(
                eq("John"), eq("John"), any(Pageable.class))).thenReturn(mockPage);

        Page<User> result = userManagementService.getAllUsers(0, 20, "John");

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByNameContainingIgnoreCaseOrPhoneContaining(
                eq("John"), eq("John"), any(Pageable.class));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllUsers_withBlankSearch_returnsAll() {
        Page<User> mockPage = new PageImpl<>(List.of(regularUser, admin));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<User> result = userManagementService.getAllUsers(0, 20, "");

        assertEquals(2, result.getTotalElements());
        verify(userRepository).findAll(any(Pageable.class));
        verify(userRepository, never()).findByNameContainingIgnoreCaseOrPhoneContaining(
                anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void getAllUsers_withNullSearch_returnsAll() {
        Page<User> mockPage = new PageImpl<>(List.of(regularUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<User> result = userManagementService.getAllUsers(0, 20, null);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllUsers_withPhoneSearch_callsSearchRepository() {
        Page<User> mockPage = new PageImpl<>(List.of(regularUser));
        when(userRepository.findByNameContainingIgnoreCaseOrPhoneContaining(
                eq("9876"), eq("9876"), any(Pageable.class))).thenReturn(mockPage);

        Page<User> result = userManagementService.getAllUsers(0, 20, "9876");

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByNameContainingIgnoreCaseOrPhoneContaining(
                eq("9876"), eq("9876"), any(Pageable.class));
    }
}
