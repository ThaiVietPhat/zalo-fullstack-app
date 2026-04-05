package com.example.backend.auth.controller;

import com.example.backend.auth.dto.AuthRequest;
import com.example.backend.auth.dto.AuthResponse;
import com.example.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Đăng ký tài khoản mới
     * POST /api/v1/auth/register
     * Body: { "email", "password", "firstName", "lastName" }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    /**
     * Đăng nhập
     * POST /api/v1/auth/login
     * Body: { "email", "password" }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Làm mới access token
     * POST /api/v1/auth/refresh
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody AuthRequest.RefreshToken request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * Đăng xuất
     * POST /api/v1/auth/logout
     * Header: Authorization: Bearer <token>
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication currentUser) {
        authService.logout(currentUser.getName());
        return ResponseEntity.ok().build();
    }
}