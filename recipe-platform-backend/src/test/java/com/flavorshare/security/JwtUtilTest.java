package com.flavorshare.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil
 * Tests JWT token generation, validation, and extraction
 */
@DisplayName("JWT Utility Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidToken() {
        // When
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);

        // When
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should extract userId from token")
    void shouldExtractUserId() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);

        // When
        Long extractedUserId = jwtUtil.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should validate correct token")
    void shouldValidateCorrectToken() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);

        // When - validateToken needs token AND username
        boolean isValid = jwtUtil.validateToken(token, TEST_USERNAME);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(null, TEST_USERNAME);
        });
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken("", TEST_USERNAME);
        });
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(malformedToken, TEST_USERNAME);
        });
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void shouldRejectTokenWithInvalidSignature() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(tamperedToken, TEST_USERNAME);
        });
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        Long id1 = 1L;
        Long id2 = 2L;

        // When
        String token1 = jwtUtil.generateToken(user1, id1);
        String token2 = jwtUtil.generateToken(user2, id2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Token should be valid immediately after creation")
    void tokenShouldBeValidImmediatelyAfterCreation() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);

        // When
        boolean isValid = jwtUtil.validateToken(token, TEST_USERNAME);

        // Then - Token should be valid immediately after creation
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void shouldHandleSpecialCharactersInUsername() {
        // Given
        String specialUsername = "user@example.com";
        Long userId = 99L;

        // When
        String token = jwtUtil.generateToken(specialUsername, userId);
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(specialUsername);
    }

    @Test
    @DisplayName("Should handle large user IDs")
    void shouldHandleLargeUserIds() {
        // Given
        Long largeUserId = Long.MAX_VALUE;

        // When
        String token = jwtUtil.generateToken(TEST_USERNAME, largeUserId);
        Long extractedId = jwtUtil.extractUserId(token);

        // Then
        assertThat(extractedId).isEqualTo(largeUserId);
    }

    @Test
    @DisplayName("Should reject token with wrong username")
    void shouldRejectTokenWithWrongUsername() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);
        String wrongUsername = "wronguser";

        // When
        boolean isValid = jwtUtil.validateToken(token, wrongUsername);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate token with correct username")
    void shouldValidateTokenWithCorrectUsername() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID);

        // When
        boolean isValid = jwtUtil.validateToken(token, TEST_USERNAME);

        // Then
        assertThat(isValid).isTrue();
    }
}