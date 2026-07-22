package com.dealspot.service;

import com.dealspot.dto.AuthRequest;
import com.dealspot.dto.AuthResponse;
import com.dealspot.entity.RefreshToken;
import com.dealspot.entity.User;
import com.dealspot.exception.AccountBannedException;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import com.dealspot.security.JwtUtil;
import net.jqwik.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property-based test for banned user authentication denial.
 *
 * Validates: Requirements REQ-USR-04
 */
@Tag("Feature: admin-panel, Property 8: Banned users are denied authentication")
class BannedUserAuthPropertyTest {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RecaptchaService recaptchaService;
    private final AuthService authService;

    BannedUserAuthPropertyTest() {
        this.userRepository = mock(UserRepository.class);
        this.refreshTokenRepository = mock(RefreshTokenRepository.class);
        this.passwordEncoder = mock(PasswordEncoder.class);
        this.jwtUtil = mock(JwtUtil.class);
        this.recaptchaService = mock(RecaptchaService.class);
        this.authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtUtil, recaptchaService);
    }

    /**
     * Property 8: Banned users are denied authentication.
     *
     * For any user with banned=true, login must throw AccountBannedException
     * regardless of whether credentials are correct.
     * For any user with banned=false and correct credentials, login must succeed.
     *
     * Validates: Requirements REQ-USR-04
     */
    @Property(tries = 200)
    void login_deniedIffBanned(
            @ForAll("bannedStatus") boolean isBanned,
            @ForAll("phoneNumbers") String phone,
            @ForAll("banReasons") String banReason
    ) {
        // Reset mocks for each trial
        reset(userRepository, passwordEncoder, recaptchaService, jwtUtil, refreshTokenRepository);

        // Always pass reCAPTCHA
        when(recaptchaService.verify(anyString())).thenReturn(true);

        String password = "correctPassword123";
        String encodedPassword = "encoded_" + password;

        User user = User.builder()
                .id(1L)
                .phone(phone)
                .password(encodedPassword)
                .name("Test User")
                .role("USER")
                .banned(isBanned)
                .banReason(isBanned ? banReason : null)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        // Mock JWT generation for successful logins
        when(jwtUtil.generateAccessToken(anyString(), any(Long.class))).thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshTokenValue()).thenReturn("mock-refresh-token");
        when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Build request
        AuthRequest request = new AuthRequest();
        request.setPhone(phone);
        request.setPassword(password);
        request.setRecaptchaToken("valid-recaptcha-token");

        if (isBanned) {
            // Banned users must be denied regardless of correct credentials
            AccountBannedException ex = assertThrows(AccountBannedException.class,
                    () -> authService.login(request),
                    "Expected AccountBannedException for banned user with phone: " + phone);
            assertTrue(ex.getMessage().contains("Account is banned"),
                    "Exception message should indicate account is banned");
        } else {
            // Non-banned users with correct credentials must succeed
            AuthResponse response = assertDoesNotThrow(
                    () -> authService.login(request),
                    "Expected successful login for non-banned user with phone: " + phone);
            assertNotNull(response, "AuthResponse should not be null for non-banned user");
            assertNotNull(response.getToken(), "Access token should not be null");
        }
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Boolean> bannedStatus() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<String> phoneNumbers() {
        // Generate 10-digit phone numbers
        return Arbitraries.strings()
                .numeric()
                .ofLength(10)
                .filter(s -> s.startsWith("9") || s.startsWith("8") || s.startsWith("7"));
    }

    @Provide
    Arbitrary<String> banReasons() {
        return Arbitraries.of("Spam", "Fraud", "Policy violation", "Abuse", "Scam listings");
    }
}
