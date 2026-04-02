package com.example.backend.controllers;

import com.example.backend.models.MessageDto;
import com.example.backend.services.FileStorageService;
import com.example.backend.services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<Void> saveMessage(
            @RequestBody MessageDto messageDto,
            Authentication currentUser) {
        messageService.sendMessage(messageDto, currentUser);
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
            @PathVariable String chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication currentUser) {  // ✅ FIX: Thêm Authentication để verify quyền truy cập
        return ResponseEntity.ok(messageService.getMessagesByChatId(chatId, page, size, currentUser));
    }

    @DeleteMapping("/{messageId}/recall")
    public ResponseEntity<Void> recallMessage(
            @PathVariable java.util.UUID messageId,
            Authentication currentUser) {
        messageService.recallMessage(messageId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/media/{filename}")
    public ResponseEntity<byte[]> getMediaFile(@PathVariable String filename) {
        byte[] file = fileStorageService.loadFile(filename);
        String contentType = fileStorageService.detectContentType(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(file);
    }
}