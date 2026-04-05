package com.example.backend.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;
import com.example.backend.shared.entity.BaseAuditingEntity;
import com.example.backend.user.entity.User;

@Entity
@Table(name = "chat",
        indexes = {
                @Index(name = "idx_chat_user1", columnList = "user1_id"),
                @Index(name = "idx_chat_user2", columnList = "user2_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Chat extends BaseAuditingEntity {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    @Transient
    public User getOtherUser(UUID currentUserId) {
        if (user1.getId().equals(currentUserId)) return user2;
        if (user2.getId().equals(currentUserId)) return user1;
        throw new IllegalArgumentException(
                "User " + currentUserId + " is not a member of chat " + this.id
        );
    }

    @Transient
    public String getChatName(UUID currentUserId) {
        User other = getOtherUser(currentUserId);
        return other.getFirstName() + " " + other.getLastName();
    }

    @Transient
    public boolean containsUser(UUID userId) {
        return user1.getId().equals(userId) || user2.getId().equals(userId);
    }
}