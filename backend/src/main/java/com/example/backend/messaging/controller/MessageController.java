package com.example.backend.messaging.controller;

import com.example.backend.messaging.dto.MessageDto;
import com.example.backend.file.service.FileStorageService;
import com.example.backend.messaging.service.MessageService;
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
    public ResponseEntity<MessageDto> saveMessage(
            @RequestBody MessageDto messageDto,
            Authentication currentUser) {
        MessageDto saved = messageService.sendMessage(messageDto, currentUser);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/upload-media/{chatId}")
    public ResponseEntity<MessageDto> uploadMedia(
            @PathVariable String chatId,
            @RequestParam("file") MultipartFile file,
            Authentication currentUser) {
        MessageDto saved = messageService.uploadMediaMessage(chatId, file, currentUser);
        return ResponseEntity.ok(saved);
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

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessageForMe(
            @PathVariable java.util.UUID messageId,
            Authentication currentUser) {
        messageService.deleteMessageForMe(messageId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/media/{filename}")
    public ResponseEntity<byte[]> getMediaFile(
            @PathVariable String filename,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] file = fileStorageService.loadFile(filename);
        String contentType = fileStorageService.detectContentType(filename);
        var builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType));
        if (download) {
            builder = builder.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        }
        return builder.body(file);
    }
}