package com.example.backend.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User extends BaseAuditingEntity {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    // ✅ THÊM MỚI: lưu mật khẩu đã hash (BCrypt)
    // nullable = true để tương thích nếu sau này vẫn muốn thêm OAuth
    @Column(name = "password")
    private String password;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(nullable = false)
    private String role = "USER";

    @Column(nullable = false)
    private boolean banned = false;

    @Column(name = "is_online", nullable = false)
    private boolean online = false;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Transient
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    @Transient
    public boolean isUserOnline() {
        return this.online;
    }

    @Transient
    public String getLastSeenText() {
        if (online) return "Đang hoạt động";
        if (lastSeen == null) return "Không xác định";

        long minutesAgo = java.time.Duration.between(lastSeen, LocalDateTime.now()).toMinutes();
        if (minutesAgo < 1) return "Vừa xong";
        if (minutesAgo < 60) return minutesAgo + " phút trước";
        if (minutesAgo < 1440) return (minutesAgo / 60) + " giờ trước";
        return (minutesAgo / 1440) + " ngày trước";
    }
}