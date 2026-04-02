package com.example.backend.services;

import com.example.backend.models.ChatDto;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    List<ChatDto> getChatByReceiverId(Authentication currentUser);
    ChatDto getChatById(UUID chatId, Authentication currentUser);
    ChatDto getOrCreateChat(UUID otherUserId, Authentication currentUser);
}
