package com.example.backend.services;

import com.example.backend.chat.entity.Chat;
import com.example.backend.user.entity.User;
import com.example.backend.shared.exception.UnauthorizedException;
import com.example.backend.chat.mapper.ChatMapper;
import com.example.backend.chat.service.ChatServiceImpl;
import com.example.backend.messaging.mapper.MessageMapper;
import com.example.backend.chat.dto.ChatDto;
import com.example.backend.chat.repository.ChatRepository;
import com.example.backend.messaging.repository.MessageRepository;
import com.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    @Mock ChatRepository chatRepository;
    @Mock ChatMapper chatMapper;
    @Mock MessageRepository messageRepository;
    @Mock UserRepository userRepository;
    @Mock MessageMapper messageMapper;
    @Mock Authentication authentication;

    @InjectMocks ChatServiceImpl chatService;

    private User user1;
    private User user2;
    private Chat chat;
    private UUID chatId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();

        user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setEmail("user1@gmail.com");
        user1.setFirstName("User");
        user1.setLastName("One");
        user1.setOnline(true);

        user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setEmail("user2@gmail.com");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setOnline(false);

        chat = new Chat();
        chat.setId(chatId);
        chat.setUser1(user1);
        chat.setUser2(user2);

        when(authentication.getName()).thenReturn("user1@gmail.com");
    }

    // ─── getChatByReceiverId ──────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy danh sách chat của user")
    void getChatByReceiverId_success() {
        ChatDto chatDto = new ChatDto();
        when(userRepository.findByEmail("user1@gmail.com")).thenReturn(Optional.of(user1));
        when(chatRepository.findAllChatsByUserId(user1.getId())).thenReturn(List.of(chat));
        when(chatMapper.toDto(chat)).thenReturn(chatDto);
        when(messageRepository.countUnreadMessages(any(), any())).thenReturn(0);
        when(messageRepository.findTop1ByChatIdOrderByCreatedDateDesc(any())).thenReturn(Optional.empty());

        List<ChatDto> result = chatService.getChatByReceiverId(authentication);

        assertThat(result).hasSize(1);
    }

    // ─── getChatById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy chat theo ID thành công")
    void getChatById_success() {
        ChatDto chatDto = new ChatDto();
        when(userRepository.findByEmail("user1@gmail.com")).thenReturn(Optional.of(user1));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMapper.toDto(chat)).thenReturn(chatDto);
        when(messageRepository.countUnreadMessages(any(), any())).thenReturn(0);
        when(messageRepository.findTop1ByChatIdOrderByCreatedDateDesc(any())).thenReturn(Optional.empty());

        ChatDto result = chatService.getChatById(chatId, authentication);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Lấy chat thất bại - không phải thành viên")
    void getChatById_accessDenied() {
        User stranger = new User();
        stranger.setId(UUID.randomUUID());
        stranger.setEmail("stranger@gmail.com");

        when(authentication.getName()).thenReturn("stranger@gmail.com");
        when(userRepository.findByEmail("stranger@gmail.com")).thenReturn(Optional.of(stranger));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.getChatById(chatId, authentication))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Access denied");
    }

    // ─── getOrCreateChat ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Tạo chat mới thành công")
    void getOrCreateChat_createsNew() {
        ChatDto chatDto = new ChatDto();
        when(userRepository.findByEmail("user1@gmail.com")).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(chatRepository.findChatBetweenTwoUsers(user1.getId(), user2.getId()))
                .thenReturn(Optional.empty());
        when(chatRepository.save(any())).thenReturn(chat);
        when(chatMapper.toDto(any())).thenReturn(chatDto);
        when(messageRepository.countUnreadMessages(any(), any())).thenReturn(0);
        when(messageRepository.findTop1ByChatIdOrderByCreatedDateDesc(any())).thenReturn(Optional.empty());

        ChatDto result = chatService.getOrCreateChat(user2.getId(), authentication);
        assertThat(result).isNotNull();
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    @DisplayName("Trả về chat đã có nếu tồn tại")
    void getOrCreateChat_returnsExisting() {
        ChatDto chatDto = new ChatDto();
        when(userRepository.findByEmail("user1@gmail.com")).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(chatRepository.findChatBetweenTwoUsers(user1.getId(), user2.getId()))
                .thenReturn(Optional.of(chat));
        when(chatMapper.toDto(any())).thenReturn(chatDto);
        when(messageRepository.countUnreadMessages(any(), any())).thenReturn(0);
        when(messageRepository.findTop1ByChatIdOrderByCreatedDateDesc(any())).thenReturn(Optional.empty());

        chatService.getOrCreateChat(user2.getId(), authentication);
        verify(chatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo chat với chính mình - thất bại")
    void getOrCreateChat_withSelf_fails() {
        when(userRepository.findByEmail("user1@gmail.com")).thenReturn(Optional.of(user1));

        assertThatThrownBy(() -> chatService.getOrCreateChat(user1.getId(), authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yourself");
    }
}
