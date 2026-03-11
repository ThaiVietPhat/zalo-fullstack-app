package com.example.backend.repositories;

import com.example.backend.Entities.Message;
import com.example.backend.enums.MessageState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChatIdOrderByCreatedDateAsc(UUID chatId);
    List<Message> findByChatIdOrderByCreatedDateDesc(UUID chatId);
    Optional<Message> findTop1ByChatIdOrderByCreatedDateDesc(UUID chatId);
    Page<Message> findByChatId(UUID chatId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId " +
            "AND m.sender.id <> :userId AND m.state <> com.example.backend.enums.MessageState.SEEN")
    int countUnreadMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);
    @Modifying
    @Query("UPDATE Message m SET m.state = :state " +
            "WHERE m.chat.id = :chatId AND m.sender.id <> :userId " +
            "AND m.state <> com.example.backend.enums.MessageState.SEEN")
    void markMessagesAsRead(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId,
            @Param("state") MessageState state
    );
}