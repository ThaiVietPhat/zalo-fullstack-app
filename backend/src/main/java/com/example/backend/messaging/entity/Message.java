package com.example.backend.messaging.entity;

import com.example.backend.messaging.enums.MessageState;
import com.example.backend.messaging.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;
import com.example.backend.shared.entity.BaseAuditingEntity;
import com.example.backend.user.entity.User;
import com.example.backend.chat.entity.Chat;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Message extends BaseAuditingEntity {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageState state = MessageState.SENT;

    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.TEXT;

    @Column(nullable = false)
    private boolean deleted = false;

    /** Người gửi tự xóa khỏi giao diện của mình */
    @Column(nullable = false)
    private boolean deletedBySender = false;

    /** Người nhận tự xóa khỏi giao diện của mình */
    @Column(nullable = false)
    private boolean deletedByReceiver = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Transient
    public User getReceiver() {
        return chat.getOtherUser(sender.getId());
    }
}