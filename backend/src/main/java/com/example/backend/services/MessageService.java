package com.example.backend.services;

import com.example.backend.models.MessageDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MessageService {

    void sendMessage(MessageDto messageDto, Authentication currentUser);

    void uploadMediaMessage(String chatId, MultipartFile file, Authentication currentUser);

    List<MessageDto> getMessagesByChatId(String chatId, int page, int size, Authentication currentUser);

    void setMessagesToSeen(String chatId, Authentication currentUser);
    void recallMessage(UUID messageId, Authentication currentUser);
}