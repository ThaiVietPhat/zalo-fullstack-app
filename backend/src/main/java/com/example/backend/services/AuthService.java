package com.example.backend.services;

import com.example.backend.Entities.User;
import com.example.backend.models.AuthRequest;
import com.example.backend.models.AuthResponse;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ─── Đăng ký ─────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(AuthRequest.Register request) {
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng: " + request.getEmail());
        }

        // Tạo user mới
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName() != null ? request.getLastName() : "");
        user.setOnline(false);
        user.setLastSeen(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("User đã đăng ký thành công: {}", savedUser.getEmail());

        return buildAuthResponse(savedUser);
    }

    // ─── Đăng nhập ───────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(AuthRequest.Login request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }

        if (user.isBanned()) {
            throw new BadCredentialsException("Tài khoản của bạn đã bị khóa");
        }

        // Cập nhật trạng thái online
        user.setOnline(true);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        log.info("User đã đăng nhập: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ─── Refresh token ────────────────────────────────────────────────────────

    public AuthResponse refreshToken(AuthRequest.RefreshToken request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));

        // Cấp access token mới và refresh token mới (rotation)
        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getId().toString(),
                user.getRole()
        );
        String newRefreshToken = jwtService.generateRefreshToken(
                user.getEmail(),
                user.getId().toString()
        );

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .online(user.isOnline())
                .build();
    }

    // ─── Đăng xuất ───────────────────────────────────────────────────────────

    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setOnline(false);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
            log.info("User đã đăng xuất: {}", email);
        });
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user.getEmail(), user.getId().toString(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getId().toString());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .online(user.isOnline())
                .build();
    }
}