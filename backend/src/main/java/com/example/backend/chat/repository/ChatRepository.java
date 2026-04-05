package com.example.backend.chat.repository;

import com.example.backend.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("SELECT c FROM Chat c " +
           "WHERE c.user1.id = :userId OR c.user2.id = :userId " +
           "ORDER BY c.lastModifiedDate DESC")
    List<Chat> findAllChatsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Chat c " +
           "WHERE (c.user1.id = :user1Id AND c.user2.id = :user2Id) " +
           "OR (c.user1.id = :user2Id AND c.user2.id = :user1Id)")
    Optional<Chat> findChatBetweenTwoUsers(@Param("user1Id") UUID user1Id, 
                                           @Param("user2Id") UUID user2Id);
}