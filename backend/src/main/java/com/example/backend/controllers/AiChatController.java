package com.example.backend.controllers;

import com.example.backend.models.AiChatRequest;
import com.example.backend.models.AiMessageDto;
import com.example.backend.services.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<AiMessageDto> chat(
            @Valid @RequestBody AiChatRequest request,
            Authentication currentUser) {
        return ResponseEntity.ok(aiChatService.sendMessage(request, currentUser));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<AiMessageDto>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication currentUser) {
        return ResponseEntity.ok(aiChatService.getHistory(page, size, currentUser));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(Authentication currentUser) {
        aiChatService.clearHistory(currentUser);
        return ResponseEntity.ok().build();
    }
}
