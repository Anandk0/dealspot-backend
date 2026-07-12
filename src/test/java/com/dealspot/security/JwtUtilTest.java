package com.dealspot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm");
        ReflectionTestUtils.setField(jwtUtil, "accessExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L);
    }

    @Test
    void generateAccessToken_shouldReturnValidToken() {
        String token = jwtUtil.generateAccessToken("9876543210", 1L);

        assertNotNull(token);
        assertTrue(token.length() > 50);
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateAccessToken("9876543210", 1L);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_shouldReturnFalseForNull() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyString() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void extractPhone_shouldReturnCorrectPhone() {
        String token = jwtUtil.generateAccessToken("9876543210", 1L);

        assertEquals("9876543210", jwtUtil.extractPhone(token));
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        String token = jwtUtil.generateAccessToken("9876543210", 42L);

        assertEquals(42L, jwtUtil.extractUserId(token));
    }

    @Test
    void generateRefreshTokenValue_shouldReturnUUID() {
        String refreshToken = jwtUtil.generateRefreshTokenValue();

        assertNotNull(refreshToken);
        assertTrue(refreshToken.contains("-")); // UUID format
        assertEquals(36, refreshToken.length());
    }

    @Test
    void generateRefreshTokenValue_shouldBeUnique() {
        String token1 = jwtUtil.generateRefreshTokenValue();
        String token2 = jwtUtil.generateRefreshTokenValue();

        assertNotEquals(token1, token2);
    }

    @Test
    void getRefreshExpirationMs_shouldReturnConfiguredValue() {
        assertEquals(604800000L, jwtUtil.getRefreshExpirationMs());
    }

    @Test
    void validateToken_shouldFailForExpiredToken() {
        // Set expiration to -1 (already expired)
        ReflectionTestUtils.setField(jwtUtil, "accessExpiration", -1L);
        String token = jwtUtil.generateAccessToken("9876543210", 1L);

        assertFalse(jwtUtil.validateToken(token));
    }
}
