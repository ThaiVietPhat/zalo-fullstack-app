package com.example.backend.services;

import com.example.backend.models.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi message real-time đến receiver qua WebSocket
     */
    public void sendMessageNotification(UUID receiverId, MessageDto messageDto) {
        log.info("Sending message notification to user: {}", receiverId);
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                messageDto
        );
    }

    /**
     * Gửi thông báo message đã được đọc
     */
    public void sendMessageSeenNotification(UUID receiverId, UUID chatId) {
        log.info("Sending message seen notification to user: {} for chat: {}", receiverId, chatId);
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/seen",
                new MessageSeenPayload(chatId)
        );
    }

    /**
     * Gửi typing indicator
     */
    public void sendTypingNotification(UUID chatId, UUID userId, boolean isTyping) {
        log.debug("User {} is {}typing in chat {}", userId, isTyping ? "" : "not ", chatId);
        messagingTemplate.convertAndSend(
                "/topic/chat/" + chatId + "/typing",
                new TypingPayload(userId, isTyping)
        );
    }

    /**
     * Gửi thông báo user online/offline status
     */
    public void sendUserStatusNotification(UUID userId, boolean isOnline) {
        log.info("User {} is now {}", userId, isOnline ? "online" : "offline");
        messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/status",
                new UserStatusPayload(userId, isOnline)
        );
    }

    // Payload classes
    public record MessageSeenPayload(UUID chatId) {}
    public record TypingPayload(UUID userId, boolean isTyping) {}
    public record UserStatusPayload(UUID userId, boolean isOnline) {}
}