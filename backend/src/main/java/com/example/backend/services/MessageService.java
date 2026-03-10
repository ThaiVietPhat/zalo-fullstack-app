package com.example.backend.services;

import com.example.backend.models.MessageDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {
    void sendMessage(MessageDto messageDto);
    void uploadMediaMessage(String chatId, MultipartFile file, Authentication currentUser);
    List<MessageDto> getMessagesByChatId(String chatId);
    void setMessagesToSeen(String chatId, Authentication currentUser);
}