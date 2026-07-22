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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RecaptchaService recaptchaService;

    public AuthResponse register(RegisterRequest request) {
        // Verify reCAPTCHA
        if (!recaptchaService.verify(request.getRecaptchaToken())) {
            throw new RuntimeException("reCAPTCHA verification failed");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already registered");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .location(request.getLocation())
                .district(request.getDistrict())
                .build();

        user = userRepository.save(user);
        return generateAuthResponse(user);
    }

    public AuthResponse login(AuthRequest request) {
        // Verify reCAPTCHA
        if (!recaptchaService.verify(request.getRecaptchaToken())) {
            throw new RuntimeException("reCAPTCHA verification failed");
        }

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is banned — reject regardless of credentials
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new AccountBannedException(user.getBanReason());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            // Token reuse detected — revoke all tokens for this user (security)
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId());
            throw new RuntimeException("Refresh token expired or revoked. Please login again.");
        }

        // Rotate: revoke old, issue new
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return generateAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getPhone(), user.getId());

        // Create refresh token
        String refreshTokenValue = jwtUtil.generateRefreshTokenValue();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenValue)
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }
}
