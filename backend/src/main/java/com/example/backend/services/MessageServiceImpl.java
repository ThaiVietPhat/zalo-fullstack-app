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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public void sendMessage(MessageDto messageDto, Authentication currentUser) {

        String email = currentUser.getName();
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Chat chat = chatRepository.findById(messageDto.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.containsUser(sender.getId())) {
            throw new RuntimeException("Access denied: You are not a member of this chat");
        }

        if (messageDto.getType() == MessageType.TEXT
                && (messageDto.getContent() == null || messageDto.getContent().isBlank())) {
            throw new RuntimeException("Message content cannot be empty");
        }

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(messageDto.getContent());
        message.setState(MessageState.SENT);
        message.setType(messageDto.getType() != null ? messageDto.getType() : MessageType.TEXT);

        Message savedMessage = messageRepository.save(message);
        MessageDto savedDto = messageMapper.toDto(savedMessage);

        User receiver = chat.getOtherUser(sender.getId());
        log.info("Sending real-time message from {} to {}", sender.getId(), receiver.getId());
        notificationService.sendMessageNotification(receiver.getId(), savedDto);
    }

    @Override
    @Transactional
    public void uploadMediaMessage(String chatId, MultipartFile file, Authentication currentUser) {
        Chat chat = chatRepository.findById(UUID.fromString(chatId))
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        String email = currentUser.getName();
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        if (!chat.containsUser(sender.getId())) {
            throw new RuntimeException("Access denied: You are not a member of this chat");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "";
        MessageType messageType;
        if (contentType.startsWith("image/")) {
            messageType = MessageType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            messageType = MessageType.VIDEO;
        } else {
            messageType = MessageType.FILE;
        }

        String filePath = fileStorageService.saveFile(file);

        Message message = new Message();
        message.setChat(chat);
        message.setContent(filePath);
        message.setState(MessageState.SENT);
        message.setType(messageType);
        message.setSender(sender);

        Message savedMessage = messageRepository.save(message);
        MessageDto savedDto = messageMapper.toDto(savedMessage);

        User receiver = chat.getOtherUser(sender.getId());
        log.info("Sending media message notification from {} to {}", sender.getId(), receiver.getId());
        notificationService.sendMessageNotification(receiver.getId(), savedDto);
    }

    @Override
    public List<MessageDto> getMessagesByChatId(String chatId, int page, int size, Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID chatUuid = UUID.fromString(chatId);
        Chat chat = chatRepository.findById(chatUuid)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.containsUser(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        Page<Message> messagePage = messageRepository.findByChatId(
                chatUuid,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        );

        return messagePage.getContent()
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

        Chat chat = chatRepository.findById(chatUuid)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.containsUser(user.getId())) {
            throw new RuntimeException("Access denied");
        }


        messageRepository.markMessagesAsRead(chatUuid, user.getId(), MessageState.SEEN);

        // Thông báo cho sender biết messages đã được đọc
        User sender = chat.getOtherUser(user.getId());
        log.info("User {} marked messages as seen in chat {}, notifying sender {}",
                user.getId(), chatUuid, sender.getId());
        notificationService.sendMessageSeenNotification(sender.getId(), chatUuid);
    }
}