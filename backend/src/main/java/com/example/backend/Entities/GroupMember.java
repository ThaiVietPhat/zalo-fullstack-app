package com.example.backend.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "group_member",
        indexes = {
                @Index(name = "idx_group_member_group", columnList = "group_id"),
                @Index(name = "idx_group_member_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_member",
                        columnNames = {"group_id", "user_id"}
                )
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GroupMember extends BaseAuditingEntity {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // true = admin nhóm, false = thành viên thường
    @Column(nullable = false)
    private boolean admin = false;

    // Factory method tiện dụng
    public static GroupMember of(Group group, User user, boolean isAdmin) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setAdmin(isAdmin);
        return member;
    }
}