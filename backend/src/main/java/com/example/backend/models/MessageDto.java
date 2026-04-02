package com.example.backend.models;

import com.example.backend.enums.MessageState;
import com.example.backend.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class MessageDto {
    private UUID id;
    private UUID chatId;
    private String content;
    private MessageState state;
    private MessageType type;
    private LocalDateTime createdAt;
    private UUID senderId;
    private UUID receiverId;
    private String mediaUrl;
    private boolean deleted;
    private List<ReactionDto> reactions;
}
