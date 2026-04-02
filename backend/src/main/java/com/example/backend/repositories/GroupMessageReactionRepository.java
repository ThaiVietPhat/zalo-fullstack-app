package com.example.backend.repositories;

import com.example.backend.Entities.GroupMessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMessageReactionRepository extends JpaRepository<GroupMessageReaction, UUID> {
    Optional<GroupMessageReaction> findByGroupMessageIdAndUserId(UUID groupMessageId, UUID userId);
    List<GroupMessageReaction> findByGroupMessageId(UUID groupMessageId);
    List<GroupMessageReaction> findByGroupMessageIdIn(Collection<UUID> groupMessageIds);

    @Transactional
    void deleteByGroupMessageIdAndUserId(UUID groupMessageId, UUID userId);
}
