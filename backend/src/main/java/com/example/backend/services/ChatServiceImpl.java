package com.example.backend.services;

import com.example.backend.Entities.Chat;
import com.example.backend.Entities.Message;
import com.example.backend.Entities.User;
import com.example.backend.enums.MessageState;
import com.example.backend.mappers.ChatMapper;
import com.example.backend.mappers.MessageMapper;
import com.example.backend.models.ChatDto;
import com.example.backend.models.MessageDto;
import com.example.backend.repositories.ChatRepository;
import com.example.backend.repositories.MessageRepository;
import com.example.backend.repositories.UserRepository;
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Chat> chats = chatRepository.findAllChatsByUserId(user.getId());

        return chats.stream()
                .map(chat -> {
                    ChatDto dto = chatMapper.toDto(chat);
                    dto.setChatName(chat.getChatName(user.getId()));
                    dto.setUnreadCount(messageRepository.countUnreadMessages(chat.getId(), user.getId()));

                    User otherUser = chat.getOtherUser(user.getId());
                    dto.setRecipientOnline(otherUser.isUserOnline());

                    List<Message> lastMessages = messageRepository.findByChatIdOrderByCreatedDateDesc(chat.getId());
                    if (!lastMessages.isEmpty()) {
                        Message last = lastMessages.get(0);
                        dto.setLastMessageType(last.getType());
                        dto.setLastMessageTime(last.getCreatedDate());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatDto getChatById(UUID chatId, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.containsUser(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        ChatDto dto = chatMapper.toDto(chat);
        dto.setChatName(chat.getChatName(user.getId()));
        dto.setUnreadCount(messageRepository.countUnreadMessages(chat.getId(), user.getId()));
        
        User otherUser = chat.getOtherUser(user.getId());
        dto.setRecipientOnline(otherUser.isUserOnline());

        return dto;
    }

    @Transactional
    public ChatDto getOrCreateChat(UUID otherUserId, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Other user not found"));

        Chat chat = chatRepository.findChatBetweenTwoUsers(user.getId(), otherUserId)
                .orElseGet(() -> {
                    Chat newChat = new Chat();
                    newChat.setUser1(user);
                    newChat.setUser2(otherUser);
                    return chatRepository.save(newChat);
                });

        ChatDto dto = chatMapper.toDto(chat);
        dto.setChatName(chat.getChatName(user.getId()));
        dto.setUnreadCount(messageRepository.countUnreadMessages(chat.getId(), user.getId()));
        dto.setRecipientOnline(otherUser.isUserOnline());

        return dto;
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessagesByChatId(UUID chatId, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.containsUser(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        List<Message> messages = messageRepository.findByChatIdOrderByCreatedDateAsc(chatId);
        return messages.stream()
                .map(messageMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(UUID chatId, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.containsUser(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        messageRepository.markMessagesAsRead(chatId, user.getId(), MessageState.RECEIVED);
    }
}