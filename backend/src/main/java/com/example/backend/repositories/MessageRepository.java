package com.example.backend.repositories;

import com.example.backend.Entities.Message;
import com.example.backend.enums.MessageState;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdDate ASC")
    List<Message> findByChatIdOrderByCreatedDateAsc(@Param("chatId") UUID chatId);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdDate DESC")
    List<Message> findByChatIdOrderByCreatedDateDesc(@Param("chatId") UUID chatId);

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.chat.id = :chatId " +
           "AND m.sender.id != :userId " +
           "AND m.state = 'SENT'")
    long countUnreadMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.state = :newState " +
           "WHERE m.chat.id = :chatId " +
           "AND m.sender.id != :userId")
    void markMessagesAsRead(@Param("chatId") UUID chatId, 
                           @Param("userId") UUID userId, 
                           @Param("newState") MessageState newState);
}