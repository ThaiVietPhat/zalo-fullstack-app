package com.example.backend.services;

import com.example.backend.chat.entity.Chat;
import com.example.backend.messaging.entity.Message;
import com.example.backend.user.entity.User;
import com.example.backend.messaging.enums.MessageState;
import com.example.backend.messaging.enums.MessageType;
import com.example.backend.messaging.mapper.MessageMapper;
import com.example.backend.messaging.dto.MessageDto;
import com.example.backend.chat.repository.ChatRepository;
import com.example.backend.messaging.repository.MessageReactionRepository;
import com.example.backend.messaging.repository.MessageRepository;
import com.example.backend.messaging.service.MessageServiceImpl;
import com.example.backend.messaging.service.NotificationService;
import com.example.backend.file.service.FileStorageService;
import com.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Unit Tests")
class MessageServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock ChatRepository chatRepository;
    @Mock MessageMapper messageMapper;
    @Mock FileStorageService fileStorageService;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock MessageReactionRepository messageReactionRepository;
    @Mock Authentication authentication;

    @InjectMocks MessageServiceImpl messageService;

    private User sender;
    private User receiver;
    private Chat chat;
    private UUID chatId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();

        sender = new User();
        sender.setId(UUID.randomUUID());
        sender.setEmail("sender@gmail.com");
        sender.setFirstName("Sender");

        receiver = new User();
        receiver.setId(UUID.randomUUID());
        receiver.setEmail("receiver@gmail.com");
        receiver.setFirstName("Receiver");

        chat = new Chat();
        chat.setUser1(sender);
        chat.setUser2(receiver);

        when(authentication.getName()).thenReturn("sender@gmail.com");
    }

    // ─── sendMessage ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Gửi tin nhắn TEXT thành công")
    void sendMessage_success() {
        MessageDto dto = new MessageDto();
        dto.setChatId(chatId);
        dto.setContent("Xin chào!");
        dto.setType(MessageType.TEXT);

        Message savedMsg = new Message();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setContent("Xin chào!");
        savedMsg.setType(MessageType.TEXT);
        savedMsg.setState(MessageState.SENT);
        savedMsg.setChat(chat);
        savedMsg.setSender(sender);

        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any())).thenReturn(savedMsg);
        when(messageMapper.toDto(any())).thenReturn(dto);

        assertThatNoException().isThrownBy(() -> messageService.sendMessage(dto, authentication));
        verify(messageRepository).save(any(Message.class));
        verify(notificationService).sendMessageNotification(eq(receiver.getEmail()), any());
    }

    @Test
    @DisplayName("Gửi tin nhắn thất bại - chat không tồn tại")
    void sendMessage_chatNotFound() {
        MessageDto dto = new MessageDto();
        dto.setChatId(chatId);
        dto.setContent("Hello");
        dto.setType(MessageType.TEXT);

        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(dto, authentication))
                .isInstanceOf(com.example.backend.shared.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Chat not found");
    }

    @Test
    @DisplayName("Gửi tin nhắn thất bại - không phải thành viên chat")
    void sendMessage_accessDenied() {
        User stranger = new User();
        stranger.setId(UUID.randomUUID());
        stranger.setEmail("stranger@gmail.com");

        when(authentication.getName()).thenReturn("stranger@gmail.com");
        when(userRepository.findByEmail("stranger@gmail.com")).thenReturn(Optional.of(stranger));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        MessageDto dto = new MessageDto();
        dto.setChatId(chatId);
        dto.setContent("Hello");
        dto.setType(MessageType.TEXT);

        assertThatThrownBy(() -> messageService.sendMessage(dto, authentication))
                .isInstanceOf(com.example.backend.shared.exception.UnauthorizedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Gửi tin nhắn thất bại - content rỗng")
    void sendMessage_emptyContent() {
        MessageDto dto = new MessageDto();
        dto.setChatId(chatId);
        dto.setContent("   ");
        dto.setType(MessageType.TEXT);

        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> messageService.sendMessage(dto, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }

    // ─── uploadMediaMessage ───────────────────────────────────────────────────

    @Test
    @DisplayName("Upload ảnh thành công")
    void uploadMedia_image_success() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");

        Message savedMsg = new Message();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setType(MessageType.IMAGE);
        savedMsg.setChat(chat);
        savedMsg.setSender(sender);

        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(fileStorageService.saveFile(file)).thenReturn("uploads/image.jpg");
        when(messageRepository.save(any())).thenReturn(savedMsg);
        when(messageMapper.toDto(any())).thenReturn(new MessageDto());

        assertThatNoException().isThrownBy(
                () -> messageService.uploadMediaMessage(chatId.toString(), file, authentication));
        verify(fileStorageService).saveFile(file);
        verify(notificationService).sendMessageNotification(eq(receiver.getEmail()), any());
    }

    @Test
    @DisplayName("Upload video thành công")
    void uploadMedia_video_success() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("video/mp4");

        Message savedMsg = new Message();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setType(MessageType.VIDEO);
        savedMsg.setChat(chat);
        savedMsg.setSender(sender);

        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(fileStorageService.saveFile(file)).thenReturn("uploads/video.mp4");
        when(messageRepository.save(any())).thenReturn(savedMsg);
        when(messageMapper.toDto(any())).thenReturn(new MessageDto());

        assertThatNoException().isThrownBy(
                () -> messageService.uploadMediaMessage(chatId.toString(), file, authentication));
    }

    // ─── getMessagesByChatId ──────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy tin nhắn theo chatId thành công")
    void getMessagesByChatId_success() {
        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setContent("Hello");

        Page<Message> page = new PageImpl<>(List.of(msg));
        MessageDto msgDto = new MessageDto();

        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdAndDeletedFalse(eq(chatId), any(Pageable.class))).thenReturn(page);
        when(messageMapper.toDto(msg)).thenReturn(msgDto);
        when(messageReactionRepository.findByMessageIdIn(any())).thenReturn(List.of());

        List<MessageDto> result = messageService.getMessagesByChatId(
                chatId.toString(), 0, 30, authentication);

        assertThat(result).hasSize(1);
    }

    // ─── setMessagesToSeen ────────────────────────────────────────────────────

    @Test
    @DisplayName("Đánh dấu đã xem thành công")
    void setMessagesToSeen_success() {
        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThatNoException().isThrownBy(
                () -> messageService.setMessagesToSeen(chatId.toString(), authentication));

        verify(messageRepository).markMessagesAsRead(chatId, sender.getId(), MessageState.SEEN);
        verify(notificationService).sendMessageSeenNotification(eq(receiver.getEmail()), eq(chatId));
    }
}