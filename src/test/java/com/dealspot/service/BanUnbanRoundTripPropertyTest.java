package com.dealspot.service;

import com.dealspot.entity.User;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import net.jqwik.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for ban/unban round-trip state restoration.
 *
 * Validates: Requirements REQ-USR-03, REQ-USR-05
 */
@Tag("Feature: admin-panel, Property 7: Ban/unban is a round-trip that restores user state")
class BanUnbanRoundTripPropertyTest {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;
    private final UserManagementService userManagementService;

    BanUnbanRoundTripPropertyTest() {
        this.userRepository = mock(UserRepository.class);
        this.refreshTokenRepository = mock(RefreshTokenRepository.class);
        this.auditService = mock(AuditService.class);
        this.userManagementService = new UserManagementService(userRepository, refreshTokenRepository, auditService);
    }

    /**
     * Property 7a: Ban then unban round-trip restores user state.
     *
     * For any non-banned user, banning then immediately unbanning SHALL result in
     * banned=false, banReason=null, bannedAt=null.
     *
     * Validates: Requirements REQ-USR-03, REQ-USR-05
     */
    @Property(tries = 100)
    void banThenUnban_restoresOriginalState(
            @ForAll("userIds") Long userId,
            @ForAll("banReasons") String banReason
    ) {
        // Reset mocks for each trial
        reset(userRepository, refreshTokenRepository, auditService);

        // Create a non-banned user
        User target = User.builder()
                .id(userId)
                .phone("9876543210")
                .password("encoded")
                .name("Test User")
                .role("USER")
                .banned(false)
                .build();

        // Actor must be ADMIN or SUPER_ADMIN
        User actor = User.builder()
                .id(999L)
                .phone("9000000001")
                .password("encoded")
                .name("Admin")
                .role("ADMIN")
                .banned(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Step 1: Ban the user
        userManagementService.banUser(userId, banReason, actor);

        // Verify user is banned with the given reason
        assertTrue(target.getBanned(), "User should be banned after banUser()");
        assertEquals(banReason, target.getBanReason(), "Ban reason should match");
        assertNotNull(target.getBannedAt(), "BannedAt should be set");

        // Step 2: Unban the user
        userManagementService.unbanUser(userId, actor);

        // Verify state is fully restored
        assertFalse(target.getBanned(), "User should not be banned after unban round-trip");
        assertNull(target.getBanReason(), "Ban reason should be null after unban");
        assertNull(target.getBannedAt(), "BannedAt should be null after unban");
    }

    /**
     * Property 7b: Unban then ban with new reason stores the new reason.
     *
     * For any banned user, unbanning then banning with a new reason SHALL result in
     * banned=true with the new reason stored.
     *
     * Validates: Requirements REQ-USR-03, REQ-USR-05
     */
    @Property(tries = 100)
    void unbanThenBan_storesNewReason(
            @ForAll("userIds") Long userId,
            @ForAll("banReasons") String originalReason,
            @ForAll("banReasons") String newReason
    ) {
        // Reset mocks for each trial
        reset(userRepository, refreshTokenRepository, auditService);

        // Create a banned user
        User target = User.builder()
                .id(userId)
                .phone("9876543210")
                .password("encoded")
                .name("Test User")
                .role("USER")
                .banned(true)
                .banReason(originalReason)
                .build();

        // Actor must be ADMIN or SUPER_ADMIN
        User actor = User.builder()
                .id(999L)
                .phone("9000000001")
                .password("encoded")
                .name("Admin")
                .role("SUPER_ADMIN")
                .banned(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Step 1: Unban the user
        userManagementService.unbanUser(userId, actor);

        // Verify user is unbanned
        assertFalse(target.getBanned(), "User should not be banned after unbanUser()");
        assertNull(target.getBanReason(), "Ban reason should be null after unban");
        assertNull(target.getBannedAt(), "BannedAt should be null after unban");

        // Step 2: Ban with new reason
        userManagementService.banUser(userId, newReason, actor);

        // Verify new ban state
        assertTrue(target.getBanned(), "User should be banned after re-ban");
        assertEquals(newReason, target.getBanReason(), "New ban reason should be stored");
        assertNotNull(target.getBannedAt(), "BannedAt should be set after re-ban");
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> banReasons() {
        return Arbitraries.of(
                "Spam", "Fraud", "Policy violation", "Abuse",
                "Scam listings", "Harassment", "Multiple accounts",
                "Illegal content", "Impersonation", "Terms of service violation"
        );
    }
}
