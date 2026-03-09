package com.example.backend.Entities;

import com.example.backend.enums.MessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import com.example.backend.enums.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Chat extends BaseAuditingEntity {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY) // Chuyển sang LAZY
    @OrderBy("createdDate DESC")
    private List<Message> messages;

    @Transient
    public String getChatName(final UUID currentUserId) {
        if (receiver.getId().equals(currentUserId)) {
            return sender.getFirstName() + " " + sender.getLastName();
        }
        return receiver.getFirstName() + " " + receiver.getLastName();
    }

    @Transient
    public long getUnreadMessages(final UUID currentUserId) {
        if (messages == null) return 0;
        return messages.stream()
                .filter(m -> m.getReceiver().getId().equals(currentUserId))
                .filter(m -> MessageState.SENT == m.getState())
                .count();
    }

    @Transient
    public String getLastMessage() {
        if (messages != null && !messages.isEmpty()) {
            Message last = messages.get(0);
            if (last.getType() != MessageType.TEXT) {
                return "Attachment"; // Nếu không phải text thì hiện Attachment
            }
            return last.getContent();
        }
        return null;
    }
}