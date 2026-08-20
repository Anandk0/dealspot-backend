package com.dealspot.service;

import com.dealspot.entity.User;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import net.jqwik.api.*;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Property-based test for authorization restriction on admin category endpoints.
 *
 * Tests that UserManagementService.checkRole correctly gates access:
 * - Non-admin roles (USER, CHECKER) are always denied
 * - Admin roles (ADMIN, SUPER_ADMIN) are always allowed
 * - Null user is always denied
 *
 * Validates: Requirements 2.4, 3.4, 4.5, 6.3
 */
@Tag("dynamic-categories")
class AdminCategoryAuthPropertyTest {

    private final UserManagementService userManagementService;

    AdminCategoryAuthPropertyTest() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        AuditService auditService = mock(AuditService.class);
        this.userManagementService = new UserManagementService(userRepository, refreshTokenRepository, auditService);
    }

    /**
     * Property 11.1: For ANY user with a non-admin role (USER, CHECKER),
     * checkRole("ADMIN", "SUPER_ADMIN") must throw AccessDeniedException.
     *
     * Validates: Requirements 2.4, 3.4, 4.5, 6.3
     */
    @Property(tries = 200)
    void nonAdminRoles_alwaysDenied(@ForAll("nonAdminUsers") User user) {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> userManagementService.checkRole(user, "ADMIN", "SUPER_ADMIN"),
                "Expected AccessDeniedException for user with role: " + user.getRole());
        assertTrue(ex.getMessage().contains("Insufficient permissions"),
                "Exception message should indicate insufficient permissions");
    }

    /**
     * Property 11.2: For ANY user with an admin role (ADMIN, SUPER_ADMIN),
     * checkRole("ADMIN", "SUPER_ADMIN") must NOT throw.
     *
     * Validates: Requirements 2.4, 3.4, 4.5, 6.3
     */
    @Property(tries = 200)
    void adminRoles_alwaysAllowed(@ForAll("adminUsers") User user) {
        assertDoesNotThrow(
                () -> userManagementService.checkRole(user, "ADMIN", "SUPER_ADMIN"),
                "Expected no exception for user with role: " + user.getRole());
    }

    /**
     * Property 11.3: A null user must always be denied access.
     *
     * Validates: Requirements 2.4, 3.4, 4.5, 6.3
     */
    @Property(tries = 50)
    void nullUser_alwaysDenied(@ForAll("adminAllowedRoles") String[] allowedRoles) {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> userManagementService.checkRole(null, allowedRoles),
                "Expected AccessDeniedException for null user");
        assertTrue(ex.getMessage().contains("Insufficient permissions"),
                "Exception message should indicate insufficient permissions");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<User> nonAdminUsers() {
        Arbitrary<String> nonAdminRoles = Arbitraries.of("USER", "CHECKER");
        return nonAdminRoles.map(role -> User.builder()
                .id(1L)
                .phone("9876543210")
                .password("password123")
                .name("Test User")
                .role(role)
                .build());
    }

    @Provide
    Arbitrary<User> adminUsers() {
        Arbitrary<String> adminRoles = Arbitraries.of("ADMIN", "SUPER_ADMIN");
        return adminRoles.map(role -> User.builder()
                .id(1L)
                .phone("9876543210")
                .password("password123")
                .name("Admin User")
                .role(role)
                .build());
    }

    @Provide
    Arbitrary<String[]> adminAllowedRoles() {
        return Arbitraries.just(new String[]{"ADMIN", "SUPER_ADMIN"});
    }
}
