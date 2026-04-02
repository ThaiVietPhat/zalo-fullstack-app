package com.example.backend.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "group_message_reaction",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_msg_reaction", columnNames = {"group_message_id", "user_id"}),
        indexes = @Index(name = "idx_greaction_message", columnList = "group_message_id")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GroupMessageReaction extends BaseAuditingEntity {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_message_id", nullable = false)
    private GroupMessage groupMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String emoji;
}
