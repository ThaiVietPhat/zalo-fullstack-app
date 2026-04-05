package com.example.backend.chat.service;

import com.example.backend.chat.entity.Chat;
import com.example.backend.user.entity.User;
import com.example.backend.shared.exception.ResourceNotFoundException;
import com.example.backend.shared.exception.UnauthorizedException;
import com.example.backend.chat.mapper.ChatMapper;
import com.example.backend.messaging.mapper.MessageMapper;
import com.example.backend.chat.dto.ChatDto;
import com.example.backend.chat.repository.ChatRepository;
import com.example.backend.messaging.repository.MessageRepository;
import com.example.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ChatDto> getChatByReceiverId(Authentication currentUser) {
        final String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return chatRepository.findAllChatsByUserId(user.getId())
                .stream()
                .map(chat -> mapChatToDto(chat, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatDto getChatById(UUID chatId, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found with id: " + chatId));

        if (!chat.containsUser(user.getId())) {
            throw new UnauthorizedException("Access denied: you are not a member of this chat");
        }

        return mapChatToDto(chat, user);
    }

    @Override
    @Transactional
    public ChatDto getOrCreateChat(UUID otherUserId, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getId().equals(otherUserId)) {
            throw new IllegalArgumentException("Cannot create chat with yourself");
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + otherUserId));

        Chat chat = chatRepository.findChatBetweenTwoUsers(user.getId(), otherUserId)
                .orElseGet(() -> {
                    Chat newChat = new Chat();
                    newChat.setUser1(user);
                    newChat.setUser2(otherUser);
                    return chatRepository.save(newChat);
                });

        return mapChatToDto(chat, user);
    }

    private ChatDto mapChatToDto(Chat chat, User currentUser) {
        ChatDto dto = chatMapper.toDto(chat);
        dto.setChatName(chat.getChatName(currentUser.getId()));
        dto.setUnreadCount(messageRepository.countUnreadMessages(chat.getId(), currentUser.getId()));

        User otherUser = chat.getOtherUser(currentUser.getId());
        dto.setRecipientId(otherUser.getId());
        dto.setRecipientEmail(otherUser.getEmail());
        dto.setAvatarUrl(otherUser.getAvatarUrl());
        dto.setRecipientOnline(otherUser.isUserOnline());
        dto.setRecipientLastSeenText(otherUser.getLastSeenText());
        messageRepository.findTop1ByChatIdOrderByCreatedDateDesc(chat.getId())
                .ifPresent(last -> {
                    dto.setLastMessage(last.getContent());
                    dto.setLastMessageType(last.getType());
                    dto.setLastMessageTime(last.getCreatedDate());
                });

        return dto;
    }
}
