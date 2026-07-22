package com.dealspot.service;

import com.dealspot.entity.User;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Property-based test for role enforcement in UserManagementService.
 *
 * Validates: Requirements REQ-AUTH-02, REQ-MOD-02, REQ-AUD-06
 */
@Tag("admin-panel")
@Tag("role-enforcement")
class RoleEnforcementPropertyTest {

    private static final List<String> ALL_ROLES = List.of("USER", "CHECKER", "ADMIN", "SUPER_ADMIN");

    private final UserManagementService service;

    RoleEnforcementPropertyTest() {
        // Create service with mocked dependencies (not needed for checkRole)
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        AuditService auditService = mock(AuditService.class);
        this.service = new UserManagementService(userRepository, refreshTokenRepository, auditService);
    }

    /**
     * Property 1: Role enforcement grants access if and only if role is sufficient.
     *
     * For any user with a valid role and any non-empty set of allowed roles,
     * checkRole allows access iff the user's role is contained in the allowed set.
     *
     * Validates: Requirements REQ-AUTH-02, REQ-MOD-02, REQ-AUD-06
     */
    @Property(tries = 200)
    void checkRole_allowsAccess_iffRoleIsInAllowedSet(
            @ForAll("validRoles") String userRole,
            @ForAll("nonEmptyRoleSets") Set<String> allowedRoles
    ) {
        User user = User.builder()
                .id(1L)
                .phone("9000000001")
                .password("password123")
                .name("Test User")
                .role(userRole)
                .banned(false)
                .build();

        String[] allowedArray = allowedRoles.toArray(new String[0]);
        boolean shouldBeAllowed = allowedRoles.contains(userRole);

        if (shouldBeAllowed) {
            // Should NOT throw
            assertDoesNotThrow(() -> service.checkRole(user, allowedArray),
                    "Expected access GRANTED for role '" + userRole + "' with allowed roles " + allowedRoles);
        } else {
            // Should throw AccessDeniedException
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> service.checkRole(user, allowedArray),
                    "Expected access DENIED for role '" + userRole + "' with allowed roles " + allowedRoles);
            assertEquals("Insufficient permissions", ex.getMessage());
        }
    }

    /**
     * Property: Null user is always denied regardless of allowed roles.
     *
     * Validates: Requirements REQ-AUTH-02
     */
    @Property(tries = 100)
    void checkRole_alwaysDenies_whenUserIsNull(
            @ForAll("nonEmptyRoleSets") Set<String> allowedRoles
    ) {
        String[] allowedArray = allowedRoles.toArray(new String[0]);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.checkRole(null, allowedArray));
        assertEquals("Insufficient permissions", ex.getMessage());
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<String> validRoles() {
        return Arbitraries.of(ALL_ROLES);
    }

    @Provide
    Arbitrary<Set<String>> nonEmptyRoleSets() {
        return Arbitraries.of(ALL_ROLES)
                .set()
                .ofMinSize(1)
                .ofMaxSize(4);
    }
}
