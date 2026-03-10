package com.example.backend.models;

import com.example.backend.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatDto {
    private UUID id;
    private UUID user1Id;
    private UUID user2Id;
    private String chatName;
    private String lastMessage;
    private MessageType lastMessageType;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private boolean recipientOnline;
}