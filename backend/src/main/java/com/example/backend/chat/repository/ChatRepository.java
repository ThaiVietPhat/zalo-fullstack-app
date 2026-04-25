package com.example.backend.chat.repository;

import com.example.backend.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    // JOIN FETCH user1 + user2 để tránh N+1 khi build ChatDto
    @Query("SELECT c FROM Chat c " +
           "LEFT JOIN FETCH c.user1 " +
           "LEFT JOIN FETCH c.user2 " +
           "WHERE (c.user1.id = :userId AND c.deletedByUser1 = false) " +
           "OR (c.user2.id = :userId AND c.deletedByUser2 = false) " +
           "ORDER BY c.lastModifiedDate DESC")
    List<Chat> findAllChatsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Chat c " +
           "WHERE (c.user1.id = :user1Id AND c.user2.id = :user2Id) " +
           "OR (c.user1.id = :user2Id AND c.user2.id = :user1Id)")
    Optional<Chat> findChatBetweenTwoUsers(@Param("user1Id") UUID user1Id, 
                                           @Param("user2Id") UUID user2Id);
}