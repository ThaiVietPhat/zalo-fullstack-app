package com.example.backend.controllers;

import com.example.backend.models.MessageDto;
import com.example.backend.services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/message")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Void> saveMessage(
            @RequestBody MessageDto messageDto) {
        messageService.sendMessage(messageDto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/upload-media/{chatId}")
    public ResponseEntity<Void> uploadMedia(
            @PathVariable String chatId,
            @RequestParam("file") MultipartFile file,
            Authentication currentUser) {
        messageService.uploadMediaMessage(chatId, file, currentUser);
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/seen/{chatId}")
    public ResponseEntity<Void> setMessagesToSeen(
            @PathVariable String chatId,
            Authentication currentUser) {
        messageService.setMessagesToSeen(chatId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageDto>> getMessagesByChatId(
            @PathVariable String chatId) {
        return ResponseEntity.ok(messageService.getMessagesByChatId(chatId));
    }
}