package com.example.backend.services;

import com.example.backend.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "zalo-clone-test-secret-key-min32chars!!";
    private static final String EMAIL   = "test@gmail.com";
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",         SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs",      86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L);
    }

    @Test
    @DisplayName("Tạo access token thành công")
    void generateAccessToken_success() {
        String token = jwtService.generateAccessToken(EMAIL, USER_ID, "USER");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Access token hợp lệ")
    void isTokenValid_validToken() {
        String token = jwtService.generateAccessToken(EMAIL, USER_ID, "USER");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Token không hợp lệ - chuỗi rác")
    void isTokenValid_invalidToken() {
        assertThat(jwtService.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    @DisplayName("Extract email từ access token")
    void extractEmail_success() {
        String token = jwtService.generateAccessToken(EMAIL, USER_ID, "USER");
        assertThat(jwtService.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Extract userId từ access token")
    void extractUserId_success() {
        String token = jwtService.generateAccessToken(EMAIL, USER_ID, "USER");
        assertThat(jwtService.extractUserId(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("isAccessToken trả về true cho access token")
    void isAccessToken_true() {
        String token = jwtService.generateAccessToken(EMAIL, USER_ID, "USER");
        assertThat(jwtService.isAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("isAccessToken trả về false cho refresh token")
    void isAccessToken_false_forRefreshToken() {
        String token = jwtService.generateRefreshToken(EMAIL, USER_ID);
        assertThat(jwtService.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("Refresh token hợp lệ")
    void generateRefreshToken_valid() {
        String token = jwtService.generateRefreshToken(EMAIL, USER_ID);
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo(EMAIL);
    }
}