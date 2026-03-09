package com.example.backend.repositories;

import com.example.backend.Entities.Chat;
import com.example.backend.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    @Query("SELECT DISTINCT c FROM Chat c WHERE c.sender.id = :userId OR c.receiver.id = :userId ORDER BY c.createdDate DESC")
    List<Chat> findAllChatsByUserId(@Param("userId") UUID userId);
    @Query("SELECT c FROM Chat c WHERE " +
            "(c.sender.id = :senderId AND c.receiver.id = :receiverId) OR " +
            "(c.sender.id = :receiverId AND c.receiver.id = :senderId)")
    Optional<Chat> findChatBetweenTwoUsers(@Param("senderId") UUID senderId, @Param("receiverId") UUID receiverId);
}