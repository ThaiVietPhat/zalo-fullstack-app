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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageMapper messageMapper;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
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

        Message savedMessage = messageRepository.save(message);
        
        // Convert to DTO để gửi qua WebSocket
        MessageDto savedDto = messageMapper.toDto(savedMessage);
        
        // Xác định receiver và gửi notification real-time
        User receiver = chat.getOtherUser(sender.getId());
        log.info("Sending real-time message from {} to {}", sender.getId(), receiver.getId());
        notificationService.sendMessageNotification(receiver.getId(), savedDto);
    }

    @Override
    @Transactional
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

        Message savedMessage = messageRepository.save(message);
        
        // Convert to DTO và gửi notification
        MessageDto savedDto = messageMapper.toDto(savedMessage);
        User receiver = chat.getOtherUser(sender.getId());
        
        log.info("Sending media message notification from {} to {}", sender.getId(), receiver.getId());
        notificationService.sendMessageNotification(receiver.getId(), savedDto);
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

        UUID chatUuid = UUID.fromString(chatId);
        
        // Mark messages as read
        messageRepository.markMessagesAsRead(
                chatUuid,
                user.getId(),
                MessageState.RECEIVED
        );
        
        // Gửi notification cho sender biết message đã được đọc
        Chat chat = chatRepository.findById(chatUuid)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        
        User sender = chat.getOtherUser(user.getId());
        log.info("User {} marked messages as read in chat {}, notifying sender {}", 
                user.getId(), chatUuid, sender.getId());
        
        notificationService.sendMessageSeenNotification(sender.getId(), chatUuid);
    }
}