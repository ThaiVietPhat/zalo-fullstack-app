package com.example.backend.services;

import com.example.backend.Entities.Chat;
import com.example.backend.Entities.Message;
import com.example.backend.Entities.User;
import com.example.backend.enums.MessageState;
import com.example.backend.enums.MessageType;
import com.example.backend.mappers.MessageMapper;
import com.example.backend.models.MessageDto;
import com.example.backend.repositories.ChatRepository;
import com.example.backend.repositories.MessageRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageMapper messageMapper;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @Override
    public void sendMessage(MessageDto messageDto) {
        Chat chat = chatRepository.findById(messageDto.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        User sender = userRepository.findById(messageDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(messageDto.getContent());
        message.setState(MessageState.SENT);
        message.setType(messageDto.getType() != null ? messageDto.getType() : MessageType.TEXT);

        messageRepository.save(message);
    }

    @Override
    public void uploadMediaMessage(String chatId, MultipartFile file, Authentication currentUser) {
        Chat chat = chatRepository.findById(UUID.fromString(chatId))
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        String filePath = fileStorageService.saveFile(file);

        String email = currentUser.getName();
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Message message = new Message();
        message.setChat(chat);
        message.setContent(filePath);
        message.setState(MessageState.SENT);
        message.setType(MessageType.IMAGE);
        message.setSender(sender);

        messageRepository.save(message);
    }

    @Override
    public List<MessageDto> getMessagesByChatId(String chatId) {
        return messageRepository.findByChatIdOrderByCreatedDateAsc(UUID.fromString(chatId))
                .stream()
                .map(messageMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setMessagesToSeen(String chatId, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        messageRepository.markMessagesAsRead(
                UUID.fromString(chatId),
                user.getId(),
                MessageState.RECEIVED
        );
    }
}