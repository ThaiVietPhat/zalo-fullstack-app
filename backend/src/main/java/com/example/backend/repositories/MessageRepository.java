package com.example.backend.repositories;

import com.example.backend.Entities.Message;
import com.example.backend.enums.MessageState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChatIdOrderByCreatedDateAsc(UUID chatId);
    Optional<Message> findTop1ByChatIdOrderByCreatedDateDesc(UUID chatId);
    Page<Message> findByChatIdAndDeletedFalse(UUID chatId, Pageable pageable);

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

    long countByDeletedFalse();

    @Query("SELECT CAST(m.createdDate AS date), COUNT(m) FROM Message m " +
            "WHERE m.createdDate >= :since AND m.deleted = false " +
            "GROUP BY CAST(m.createdDate AS date) ORDER BY CAST(m.createdDate AS date) ASC")
    List<Object[]> countDailyMessages(@Param("since") LocalDateTime since);

    @Query("SELECT m.sender.id, COUNT(m) FROM Message m WHERE m.deleted = false " +
            "GROUP BY m.sender.id ORDER BY COUNT(m) DESC")
    List<Object[]> findTopSenderIds(Pageable pageable);
}
