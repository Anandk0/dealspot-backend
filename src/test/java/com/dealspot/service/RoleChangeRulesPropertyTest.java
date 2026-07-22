package com.dealspot.service;

import com.dealspot.entity.User;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property 2: Role change rules are comprehensive and non-bypassable.
 *
 * For any actor and target user, role assignment SHALL succeed only when ALL of the following hold:
 * (a) actor has role SUPER_ADMIN,
 * (b) target is a different user than actor,
 * (c) the new role is one of USER, CHECKER, or ADMIN (never SUPER_ADMIN).
 * In all other cases, the operation SHALL throw an exception.
 *
 * Validates: Requirements REQ-AUTH-03, REQ-AUTH-05, REQ-USR-06, REQ-USR-07
 */
@Tag("Feature: admin-panel, Property 2: Role change rules are comprehensive and non-bypassable")
class RoleChangeRulesPropertyTest {

    private static final List<String> ALL_ROLES = List.of("USER", "CHECKER", "ADMIN", "SUPER_ADMIN");
    private static final List<String> VALID_NEW_ROLES = List.of("USER", "CHECKER", "ADMIN");
    private static final List<String> INVALID_NEW_ROLES = List.of("SUPER_ADMIN", "MODERATOR", "", "OWNER", "ROOT");

    private UserManagementService createService(UserRepository userRepository) {
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        return new UserManagementService(userRepository, refreshTokenRepository, auditService);
    }

    @Provide
    Arbitrary<String> actorRoles() {
        return Arbitraries.of(ALL_ROLES);
    }

    @Provide
    Arbitrary<String> allPossibleNewRoles() {
        List<String> combined = List.of("USER", "CHECKER", "ADMIN", "SUPER_ADMIN", "MODERATOR", "", "OWNER", "ROOT");
        return Arbitraries.of(combined);
    }

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 100L);
    }

    /**
     * Property: Role change succeeds if and only if actor is SUPER_ADMIN,
     * target is different from actor, and newRole is in {USER, CHECKER, ADMIN}.
     */
    @Property(tries = 200)
    void roleChangeSucceedsOnlyWhenAllConditionsMet(
            @ForAll("actorRoles") String actorRole,
            @ForAll("userIds") Long actorId,
            @ForAll("userIds") Long targetId,
            @ForAll("allPossibleNewRoles") String newRole) {

        boolean isSuperAdmin = "SUPER_ADMIN".equals(actorRole);
        boolean isDifferentUser = !actorId.equals(targetId);
        boolean isValidRole = VALID_NEW_ROLES.contains(newRole);

        boolean shouldSucceed = isSuperAdmin && isDifferentUser && isValidRole;

        User actor = User.builder()
                .id(actorId)
                .phone("90000000" + (actorId % 100))
                .name("Actor")
                .role(actorRole)
                .banned(false)
                .password("hashed")
                .build();

        User target = User.builder()
                .id(targetId)
                .phone("80000000" + (targetId % 100))
                .name("Target")
                .role("USER")
                .banned(false)
                .password("hashed")
                .build();

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenReturn(target);

        UserManagementService service = createService(userRepository);

        if (shouldSucceed) {
            assertDoesNotThrow(() -> service.changeUserRole(targetId, newRole, actor),
                    String.format("Expected success for actorRole=%s, actorId=%d, targetId=%d, newRole=%s",
                            actorRole, actorId, targetId, newRole));
            verify(userRepository).save(any(User.class));
        } else {
            assertThrows(Exception.class, () -> service.changeUserRole(targetId, newRole, actor),
                    String.format("Expected exception for actorRole=%s, actorId=%d, targetId=%d, newRole=%s",
                            actorRole, actorId, targetId, newRole));
        }
    }

    /**
     * Property: Non-SUPER_ADMIN actors always get AccessDeniedException.
     */
    @Property(tries = 100)
    void nonSuperAdminAlwaysDenied(
            @ForAll("userIds") Long actorId,
            @ForAll("userIds") Long targetId,
            @ForAll("allPossibleNewRoles") String newRole) {

        List<String> nonSuperRoles = List.of("USER", "CHECKER", "ADMIN");

        for (String actorRole : nonSuperRoles) {
            User actor = User.builder()
                    .id(actorId)
                    .phone("90000000" + (actorId % 100))
                    .name("Actor")
                    .role(actorRole)
                    .banned(false)
                    .password("hashed")
                    .build();

            UserRepository userRepository = Mockito.mock(UserRepository.class);
            UserManagementService service = createService(userRepository);

            assertThrows(AccessDeniedException.class,
                    () -> service.changeUserRole(targetId, newRole, actor),
                    String.format("Expected AccessDeniedException for actorRole=%s", actorRole));
        }
    }

    /**
     * Property: Self-targeting always fails (even for SUPER_ADMIN with valid role).
     */
    @Property(tries = 100)
    void selfTargetAlwaysFails(@ForAll("userIds") Long userId) {

        for (String newRole : VALID_NEW_ROLES) {
            User actor = User.builder()
                    .id(userId)
                    .phone("90000000" + (userId % 100))
                    .name("Super")
                    .role("SUPER_ADMIN")
                    .banned(false)
                    .password("hashed")
                    .build();

            UserRepository userRepository = Mockito.mock(UserRepository.class);
            UserManagementService service = createService(userRepository);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.changeUserRole(userId, newRole, actor),
                    String.format("Expected IllegalArgumentException for self-target userId=%d, newRole=%s", userId, newRole));
            assertEquals("Cannot change own role", ex.getMessage());
        }
    }

    /**
     * Property: SUPER_ADMIN can never be assigned as a new role.
     */
    @Property(tries = 100)
    void superAdminRoleCanNeverBeAssigned(
            @ForAll("userIds") Long actorId,
            @ForAll("userIds") Long targetId) {

        Assume.that(!actorId.equals(targetId));

        User actor = User.builder()
                .id(actorId)
                .phone("90000000" + (actorId % 100))
                .name("Super")
                .role("SUPER_ADMIN")
                .banned(false)
                .password("hashed")
                .build();

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UserManagementService service = createService(userRepository);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changeUserRole(targetId, "SUPER_ADMIN", actor),
                "Expected IllegalArgumentException when assigning SUPER_ADMIN role");
        assertTrue(ex.getMessage().contains("Invalid role") || ex.getMessage().contains("Cannot assign SUPER_ADMIN"));
    }

    /**
     * Property: Invalid role strings always cause failure for SUPER_ADMIN acting on different user.
     */
    @Property(tries = 100)
    void invalidRolesAlwaysFail(
            @ForAll("userIds") Long actorId,
            @ForAll("userIds") Long targetId) {

        Assume.that(!actorId.equals(targetId));

        User actor = User.builder()
                .id(actorId)
                .phone("90000000" + (actorId % 100))
                .name("Super")
                .role("SUPER_ADMIN")
                .banned(false)
                .password("hashed")
                .build();

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UserManagementService service = createService(userRepository);

        for (String invalidRole : INVALID_NEW_ROLES) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.changeUserRole(targetId, invalidRole, actor),
                    String.format("Expected IllegalArgumentException for invalid role: '%s'", invalidRole));
            assertTrue(ex.getMessage().contains("Invalid role") || ex.getMessage().contains("Cannot assign"),
                    "Expected error message about invalid role, got: " + ex.getMessage());
        }
    }
}
