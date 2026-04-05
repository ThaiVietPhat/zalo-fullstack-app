package com.example.backend.services;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend.auth.dto.AuthRequest;
import com.example.backend.auth.dto.AuthResponse;
import com.example.backend.auth.service.AuthService;
import com.example.backend.security.service.JwtService;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("test@gmail.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        mockUser.setOnline(false);
    }

    // ─── Register ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Đăng ký thành công")
    void register_success() {
        AuthRequest.Register req = AuthRequest.Register.builder()
                .email("new@gmail.com").password("123456")
                .firstName("New").lastName("User").build();

        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(mockUser);

        authService.register(req);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại - email đã tồn tại")
    void register_emailAlreadyExists() {
        AuthRequest.Register req = AuthRequest.Register.builder()
                .email("test@gmail.com").password("123456")
                .firstName("Test").lastName("User").build();

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email đã được sử dụng");

        verify(userRepository, never()).save(any());
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Đăng nhập thành công")
    void login_success() {
        AuthRequest.Login req = AuthRequest.Login.builder()
                .email("test@gmail.com").password("123456").build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(true);
        when(userRepository.save(any())).thenReturn(mockUser);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        AuthResponse res = authService.login(req);

        assertThat(res.getAccessToken()).isNotBlank();
        assertThat(res.getEmail()).isEqualTo("test@gmail.com");
    }

    @Test
    @DisplayName("Đăng nhập thất bại - email không tồn tại")
    void login_emailNotFound() {
        AuthRequest.Login req = AuthRequest.Login.builder()
                .email("noone@gmail.com").password("123456").build();

        when(userRepository.findByEmail("noone@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Đăng nhập thất bại - sai mật khẩu")
    void login_wrongPassword() {
        AuthRequest.Login req = AuthRequest.Login.builder()
                .email("test@gmail.com").password("wrongpass").build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpass", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh token thành công")
    void refreshToken_success() {
        AuthRequest.RefreshToken req = new AuthRequest.RefreshToken("valid-refresh-token");

        when(jwtService.isTokenValid("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-refresh-token")).thenReturn("test@gmail.com");
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("new-refresh-token");

        AuthResponse res = authService.refreshToken(req);

        assertThat(res.getAccessToken()).isEqualTo("new-access-token");
        assertThat(res.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("Refresh token thất bại - token không hợp lệ")
    void refreshToken_invalidToken() {
        AuthRequest.RefreshToken req = new AuthRequest.RefreshToken("expired-token");

        when(jwtService.isTokenValid("expired-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Đăng xuất thành công")
    void logout_success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        authService.logout("test@gmail.com");

        assertThat(mockUser.isOnline()).isFalse();
        verify(userRepository).save(mockUser);
    }
}