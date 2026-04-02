package com.example.backend.controllers;

import com.example.backend.models.ReactionDto;
import com.example.backend.services.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    // ─── Reaction tin nhắn 1-1 ───────────────────────────────────────────────

    @PostMapping("/message/{messageId}/reactions")
    public ResponseEntity<Void> reactToMessage(
            @PathVariable UUID messageId,
            @RequestParam String emoji,
            Authentication currentUser) {
        reactionService.reactToMessage(messageId, emoji, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/message/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @PathVariable UUID messageId,
            Authentication currentUser) {
        reactionService.removeReactionFromMessage(messageId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/message/{messageId}/reactions")
    public ResponseEntity<List<ReactionDto>> getMessageReactions(@PathVariable UUID messageId) {
        return ResponseEntity.ok(reactionService.getMessageReactions(messageId));
    }

    // ─── Reaction tin nhắn nhóm ──────────────────────────────────────────────

    @PostMapping("/group-message/{messageId}/reactions")
    public ResponseEntity<Void> reactToGroupMessage(
            @PathVariable UUID messageId,
            @RequestParam String emoji,
            Authentication currentUser) {
        reactionService.reactToGroupMessage(messageId, emoji, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/group-message/{messageId}/reactions")
    public ResponseEntity<Void> removeGroupReaction(
            @PathVariable UUID messageId,
            Authentication currentUser) {
        reactionService.removeReactionFromGroupMessage(messageId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/group-message/{messageId}/reactions")
    public ResponseEntity<List<ReactionDto>> getGroupMessageReactions(@PathVariable UUID messageId) {
        return ResponseEntity.ok(reactionService.getGroupMessageReactions(messageId));
    }
}
