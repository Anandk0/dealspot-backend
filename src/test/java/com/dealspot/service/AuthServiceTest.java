package com.dealspot.service;

import com.dealspot.dto.AuthRequest;
import com.dealspot.dto.AuthResponse;
import com.dealspot.dto.RegisterRequest;
import com.dealspot.entity.RefreshToken;
import com.dealspot.entity.User;
import com.dealspot.exception.AccountBannedException;
import com.dealspot.repository.RefreshTokenRepository;
import com.dealspot.repository.UserRepository;
import com.dealspot.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RecaptchaService recaptchaService;

    @InjectMocks private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phone("9876543210")
                .name("Test User")
                .password("encoded_password")
                .role("USER")
                .banned(false)
                .build();
    }

    @Test
    void register_shouldSucceed_whenPhoneIsNew() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("9876543210");
        request.setPassword("password123");
        request.setName("Test User");

        when(recaptchaService.verify(any())).thenReturn(true);
        when(userRepository.existsByPhone("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(anyString(), anyLong())).thenReturn("access_token");
        when(jwtUtil.generateRefreshTokenValue()).thenReturn("refresh_token");
        when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.getToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(1L, response.getUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldFail_whenPhoneAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("9876543210");
        request.setPassword("password123");
        request.setName("Test User");

        when(recaptchaService.verify(any())).thenReturn(true);
        when(userRepository.existsByPhone("9876543210")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(request));

        assertEquals("Phone number already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldFail_whenRecaptchaFails() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("9876543210");
        request.setPassword("password123");
        request.setName("Test User");
        request.setRecaptchaToken("bad_token");

        when(recaptchaService.verify("bad_token")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(request));

        assertEquals("reCAPTCHA verification failed", exception.getMessage());
    }

    @Test
    void login_shouldSucceed_withCorrectCredentials() {
        AuthRequest request = new AuthRequest();
        request.setPhone("9876543210");
        request.setPassword("password123");

        when(recaptchaService.verify(any())).thenReturn(true);
        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyLong())).thenReturn("access_token");
        when(jwtUtil.generateRefreshTokenValue()).thenReturn("refresh_token");
        when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getToken());
        assertEquals("Test User", response.getName());
    }

    @Test
    void login_shouldFail_withWrongPassword() {
        AuthRequest request = new AuthRequest();
        request.setPhone("9876543210");
        request.setPassword("wrong_password");

        when(recaptchaService.verify(any())).thenReturn(true);
        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(request));

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void login_shouldFail_whenUserNotFound() {
        AuthRequest request = new AuthRequest();
        request.setPhone("0000000000");
        request.setPassword("password");

        when(recaptchaService.verify(any())).thenReturn(true);
        when(userRepository.findByPhone("0000000000")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void refreshToken_shouldSucceed_withValidToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("valid_refresh")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid_refresh")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any())).thenReturn(refreshToken);
        when(jwtUtil.generateAccessToken(anyString(), anyLong())).thenReturn("new_access");
        when(jwtUtil.generateRefreshTokenValue()).thenReturn("new_refresh");
        when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.refreshToken("valid_refresh");

        assertNotNull(response);
        assertEquals("new_access", response.getToken());
        assertEquals("new_refresh", response.getRefreshToken());
        assertTrue(refreshToken.getRevoked()); // Old token should be revoked
    }

    @Test
    void refreshToken_shouldFail_whenTokenIsRevoked() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("revoked_token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true) // Already revoked
                .build();

        when(refreshTokenRepository.findByToken("revoked_token")).thenReturn(Optional.of(refreshToken));

        assertThrows(RuntimeException.class,
                () -> authService.refreshToken("revoked_token"));

        // Should revoke ALL tokens for this user (reuse detection)
        verify(refreshTokenRepository).revokeAllByUserId(testUser.getId());
    }

    @Test
    void refreshToken_shouldFail_whenTokenIsExpired() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("expired_token")
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired_token")).thenReturn(Optional.of(refreshToken));

        assertThrows(RuntimeException.class,
                () -> authService.refreshToken("expired_token"));
    }

    @Test
    void logout_shouldRevokeAllTokens() {
        authService.logout(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    void login_shouldFail_whenUserIsBanned() {
        User bannedUser = User.builder()
                .id(2L)
                .phone("9876543211")
                .name("Banned User")
                .password("encoded_password")
                .role("USER")
                .banned(true)
                .banReason("Spamming listings")
                .build();

        AuthRequest request = new AuthRequest();
        request.setPhone("9876543211");
        request.setPassword("password123");

        when(recaptchaService.verify(any())).thenReturn(true);
        when(userRepository.findByPhone("9876543211")).thenReturn(Optional.of(bannedUser));

        AccountBannedException exception = assertThrows(AccountBannedException.class,
                () -> authService.login(request));

        assertEquals("Account is banned: Spamming listings", exception.getMessage());
        assertEquals("Spamming listings", exception.getBanReason());
        // Password should NOT be checked for banned users
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldFail_whenUserIsBanned_withNullReason() {
        User bannedUser = User.builder()
                .id(3L)
                .phone("9876543212")
                .name("Banned User No Reason")
                .password("encoded_password")
                .role("USER")
                .banned(true)
                .banReason(null)
                .build();

        AuthRequest request = new AuthRequest();
        request.setPhone("9876543212");
        request.setPassword("password123");

        when(recaptchaService.verify(any())).thenReturn(true);
        when(userRepository.findByPhone("9876543212")).thenReturn(Optional.of(bannedUser));

        AccountBannedException exception = assertThrows(AccountBannedException.class,
                () -> authService.login(request));

        assertEquals("Account is banned: No reason provided", exception.getMessage());
    }
}
