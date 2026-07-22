package com.dealspot.service;

import com.dealspot.dto.AuthRequest;
import com.dealspot.dto.AuthResponse;
import com.dealspot.entity.RefreshToken;
import com.dealspot.entity.User;
import com.dealspot.exception.AccountBannedException;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import com.dealspot.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests verifying the interaction between UserManagementService (ban/unban)
 * and AuthService (login). Both services share mock repositories to simulate real state changes.
 *
 * Validates: REQ-USR-03, REQ-USR-04, REQ-USR-05, REQ-USR-08
 */
@ExtendWith(MockitoExtension.class)
class UserBanAuthFlowIntegrationTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RecaptchaService recaptchaService;

    private UserManagementService userManagementService;
    private AuthService authService;

    private User targetUser;
    private User adminActor;

    @BeforeEach
    void setUp() {
        // Both services share the same mock repositories to simulate state propagation
        userManagementService = new UserManagementService(userRepository, refreshTokenRepository, auditService);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtUtil, recaptchaService);

        targetUser = User.builder()
                .id(5L)
                .phone("9876543210")
                .password("encoded_password")
                .name("Test User")
                .role("USER")
                .banned(false)
                .build();

        adminActor = User.builder()
                .id(1L)
                .phone("9000000001")
                .name("Admin")
                .role("ADMIN")
                .banned(false)
                .build();
    }

    @Test
    @DisplayName("Ban user → login rejected → unban → login succeeds")
    void banUser_loginRejected_unbanUser_loginSucceeds() {
        // Arrange: mock repository lookups to return the shared user object
        when(userRepository.findById(5L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(targetUser));
        when(recaptchaService.verify(any())).thenReturn(true);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyLong())).thenReturn("mock-token");
        when(jwtUtil.generateRefreshTokenValue()).thenReturn("mock-refresh");
        when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Step 1: Ban the user
        userManagementService.banUser(5L, "Violation of terms", adminActor);
        assertTrue(targetUser.getBanned(), "User should be banned after banUser()");

        // Step 2: Attempt login — should throw AccountBannedException
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setPhone("9876543210");
        loginRequest.setPassword("password123");
        loginRequest.setRecaptchaToken("valid-token");

        assertThrows(AccountBannedException.class,
                () -> authService.login(loginRequest),
                "Banned user login should throw AccountBannedException");

        // Step 3: Unban the user
        userManagementService.unbanUser(5L, adminActor);
        assertFalse(targetUser.getBanned(), "User should not be banned after unbanUser()");

        // Step 4: Attempt login again — should succeed
        AuthResponse response = authService.login(loginRequest);
        assertNotNull(response, "Login should succeed after unbanning");
        assertEquals("mock-token", response.getToken());
    }

    @Test
    @DisplayName("Ban user → refresh tokens are revoked")
    void banUser_refreshTokensRevoked() {
        // Arrange
        when(userRepository.findById(5L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Ban the user
        userManagementService.banUser(5L, "Spamming", adminActor);

        // Assert: refresh tokens revoked for the banned user
        verify(refreshTokenRepository).revokeAllByUserId(5L);
        assertTrue(targetUser.getBanned());
    }
}
