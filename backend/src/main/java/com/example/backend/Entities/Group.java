package com.example.backend.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "`group`",
        indexes = {
                @Index(name = "idx_group_created_by", columnList = "created_by")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Group extends BaseAuditingEntity {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Người tạo nhóm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    @Transient
    public boolean isMember(UUID userId) {
        return members.stream().anyMatch(m -> m.getUser().getId().equals(userId));
    }

    @Transient
    public boolean isAdmin(UUID userId) {
        return members.stream()
                .anyMatch(m -> m.getUser().getId().equals(userId) && m.isAdmin());
    }
}