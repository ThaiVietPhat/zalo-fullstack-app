package com.example.backend.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms:86400000}") // mặc định 24 giờ
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // mặc định 7 ngày
    private long refreshExpirationMs;

    // ─── Tạo access token ────────────────────────────────────────────────────

    public String generateAccessToken(String email, String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("email", email);
        claims.put("type", "access");
        claims.put("role", role != null ? role : "USER");
        return buildToken(claims, email, expirationMs);
    }

    // ─── Tạo refresh token ───────────────────────────────────────────────────

    public String generateRefreshToken(String email, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("email", email);
        claims.put("type", "refresh");
        return buildToken(claims, email, refreshExpirationMs);
    }

    // ─── Xây dựng token ──────────────────────────────────────────────────────

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── Verify và extract claims ────────────────────────────────────────────

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("Token không hợp lệ: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String type = (String) extractAllClaims(token).get("type");
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return (String) extractAllClaims(token).get("email");
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        try {
            Object role = extractAllClaims(token).get("role");
            return role != null ? role.toString() : "USER";
        } catch (Exception e) {
            return "USER";
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}