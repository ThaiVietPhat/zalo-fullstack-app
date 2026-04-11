package com.example.backend.messaging.service;

import com.example.backend.messaging.dto.MessageDto;
import com.example.backend.reaction.dto.ReactionDto;
import com.example.backend.user.dto.FriendRequestDto;
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

    public void sendMessageNotification(String recipientEmail, MessageDto messageDto) {
        log.info("Sending message notification to user: {}", recipientEmail);
        messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/messages",
                messageDto
        );
    }

    public void sendChatBroadcast(UUID chatId, MessageDto messageDto) {
        log.info("Broadcasting message to chat topic: {}", chatId);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageDto);
    }

    public void sendMessageDeliveredNotification(String senderEmail, UUID chatId) {
        log.info("Sending message delivered notification to user: {} for chat: {}", senderEmail, chatId);
        messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/delivered",
                new MessageDeliveredPayload(chatId)
        );
    }

    public void sendMessageSeenNotification(String recipientEmail, UUID chatId) {
        log.info("Sending message seen notification to user: {} for chat: {}", recipientEmail, chatId);
        messagingTemplate.convertAndSendToUser(
                recipientEmail,
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

    public void sendMessageRecalledNotification(String recipientEmail, UUID messageId, UUID chatId) {
        log.info("Sending message recalled notification to user: {} for message: {}", recipientEmail, messageId);
        messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/message-recalled",
                new MessageRecalledPayload(messageId, chatId)
        );
    }

    public void sendReactionNotification(String recipientEmail, UUID messageId, UUID chatId, List<ReactionDto> reactions) {
        messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/reactions",
                new ReactionEventPayload(messageId, chatId, reactions)
        );
    }

    public void sendFriendRequestNotification(String receiverEmail, FriendRequestDto dto) {
        log.info("Sending friend request notification to user: {}", receiverEmail);
        messagingTemplate.convertAndSendToUser(
                receiverEmail,
                "/queue/friend-request",
                dto
        );
    }

    public void sendForceLogout(String email, String reason) {
        log.info("Sending force-logout to user: {} — reason: {}", email, reason);
        messagingTemplate.convertAndSendToUser(
                email,
                "/queue/force-logout",
                new ForceLogoutPayload(reason)
        );
    }

    public void sendFriendRequestAcceptedNotification(String senderEmail, FriendRequestDto dto) {
        log.info("Sending friend request accepted notification to user: {}", senderEmail);
        messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/friend-request-accepted",
                dto
        );
    }

    public record ForceLogoutPayload(String reason) {}
    public record MessageDeliveredPayload(UUID chatId) {}
    public record MessageSeenPayload(UUID chatId) {}
    public record TypingPayload(UUID userId, boolean isTyping) {}
    public record UserStatusPayload(UUID userId, boolean isOnline) {}
    public record MessageRecalledPayload(UUID messageId, UUID chatId) {}
    public record ReactionEventPayload(UUID messageId, UUID chatId, List<ReactionDto> reactions) {}
}
