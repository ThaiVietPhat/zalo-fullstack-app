package com.example.backend.services;

import com.example.backend.Entities.*;
import com.example.backend.exceptions.ResourceNotFoundException;
import com.example.backend.exceptions.UnauthorizedException;
import com.example.backend.models.ReactionDto;
import com.example.backend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final MessageReactionRepository messageReactionRepository;
    private final GroupMessageReactionRepository groupMessageReactionRepository;
    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Reaction tin nhắn 1-1 ───────────────────────────────────────────────

    @Transactional
    public void reactToMessage(UUID messageId, String emoji, Authentication auth) {
        User user = getUser(auth);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        if (!message.getChat().containsUser(user.getId())) {
            throw new UnauthorizedException("Access denied");
        }

        messageReactionRepository.findByMessageIdAndUserId(messageId, user.getId())
                .ifPresentOrElse(
                        reaction -> {
                            if (reaction.getEmoji().equals(emoji)) {
                                // Cùng emoji → xóa (toggle off)
                                messageReactionRepository.delete(reaction);
                            } else {
                                // Khác emoji → cập nhật
                                reaction.setEmoji(emoji);
                                messageReactionRepository.save(reaction);
                            }
                        },
                        () -> {
                            MessageReaction reaction = new MessageReaction();
                            reaction.setMessage(message);
                            reaction.setUser(user);
                            reaction.setEmoji(emoji);
                            messageReactionRepository.save(reaction);
                        }
                );

        List<ReactionDto> reactions = getMessageReactions(messageId);
        User receiver = message.getChat().getOtherUser(user.getId());
        notificationService.sendReactionNotification(receiver.getId(), messageId, message.getChat().getId(), reactions);
    }

    @Transactional
    public void removeReactionFromMessage(UUID messageId, Authentication auth) {
        User user = getUser(auth);
        messageReactionRepository.deleteByMessageIdAndUserId(messageId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<ReactionDto> getMessageReactions(UUID messageId) {
        return messageReactionRepository.findByMessageId(messageId)
                .stream()
                .map(r -> ReactionDto.builder()
                        .id(r.getId())
                        .userId(r.getUser().getId())
                        .userFullName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                        .emoji(r.getEmoji())
                        .createdDate(r.getCreatedDate())
                        .build())
                .collect(Collectors.toList());
    }

    // ─── Reaction tin nhắn nhóm ──────────────────────────────────────────────

    @Transactional
    public void reactToGroupMessage(UUID groupMessageId, String emoji, Authentication auth) {
        User user = getUser(auth);
        GroupMessage message = groupMessageRepository.findById(groupMessageId)
                .orElseThrow(() -> new ResourceNotFoundException("Group message not found: " + groupMessageId));

        if (!message.getGroup().isMember(user.getId())) {
            throw new UnauthorizedException("Access denied");
        }

        groupMessageReactionRepository.findByGroupMessageIdAndUserId(groupMessageId, user.getId())
                .ifPresentOrElse(
                        reaction -> {
                            if (reaction.getEmoji().equals(emoji)) {
                                groupMessageReactionRepository.delete(reaction);
                            } else {
                                reaction.setEmoji(emoji);
                                groupMessageReactionRepository.save(reaction);
                            }
                        },
                        () -> {
                            GroupMessageReaction reaction = new GroupMessageReaction();
                            reaction.setGroupMessage(message);
                            reaction.setUser(user);
                            reaction.setEmoji(emoji);
                            groupMessageReactionRepository.save(reaction);
                        }
                );

        List<ReactionDto> reactions = getGroupMessageReactions(groupMessageId);
        messagingTemplate.convertAndSend("/topic/group/" + message.getGroup().getId(),
                new ReactionGroupEvent(groupMessageId, reactions));
    }

    @Transactional
    public void removeReactionFromGroupMessage(UUID groupMessageId, Authentication auth) {
        User user = getUser(auth);
        groupMessageReactionRepository.deleteByGroupMessageIdAndUserId(groupMessageId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<ReactionDto> getGroupMessageReactions(UUID groupMessageId) {
        return groupMessageReactionRepository.findByGroupMessageId(groupMessageId)
                .stream()
                .map(r -> ReactionDto.builder()
                        .id(r.getId())
                        .userId(r.getUser().getId())
                        .userFullName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                        .emoji(r.getEmoji())
                        .createdDate(r.getCreatedDate())
                        .build())
                .collect(Collectors.toList());
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public record ReactionGroupEvent(UUID messageId, List<ReactionDto> reactions) {}
}
