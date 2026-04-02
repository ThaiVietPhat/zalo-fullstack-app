package com.example.backend.services;

import com.example.backend.models.MessageDto;
import com.example.backend.models.ReactionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessageNotification(UUID receiverId, MessageDto messageDto) {
        log.info("Sending message notification to user: {}", receiverId);
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                messageDto
        );
    }


    public void sendMessageSeenNotification(UUID receiverId, UUID chatId) {
        log.info("Sending message seen notification to user: {} for chat: {}", receiverId, chatId);
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/seen",
                new MessageSeenPayload(chatId)
        );
    }

    public void sendTypingNotification(UUID chatId, UUID userId, boolean isTyping) {
        log.debug("User {} is {}typing in chat {}", userId, isTyping ? "" : "not ", chatId);
        messagingTemplate.convertAndSend(
                "/topic/chat/" + chatId + "/typing",
                new TypingPayload(userId, isTyping)
        );
    }


    public void sendUserStatusNotification(UUID userId, boolean isOnline) {
        log.info("User {} is now {}", userId, isOnline ? "online" : "offline");
        messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/status",
                new UserStatusPayload(userId, isOnline)
        );
    }

    public void sendMessageRecalledNotification(UUID receiverId, UUID messageId, UUID chatId) {
        log.info("Sending message recalled notification to user: {} for message: {}", receiverId, messageId);
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/message-recalled",
                new MessageRecalledPayload(messageId, chatId)
        );
    }

    public void sendReactionNotification(UUID receiverId, UUID messageId, UUID chatId, List<ReactionDto> reactions) {
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/reactions",
                new ReactionEventPayload(messageId, chatId, reactions)
        );
    }

    public record MessageSeenPayload(UUID chatId) {}
    public record TypingPayload(UUID userId, boolean isTyping) {}
    public record UserStatusPayload(UUID userId, boolean isOnline) {}
    public record MessageRecalledPayload(UUID messageId, UUID chatId) {}
    public record ReactionEventPayload(UUID messageId, UUID chatId, List<ReactionDto> reactions) {}
}