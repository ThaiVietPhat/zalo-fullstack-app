package com.example.backend.models;

import com.example.backend.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupMessageDto {
    private UUID id;
    private String content;
    private MessageType type;
    private UUID groupId;
    private UUID senderId;
    private String senderName;
    private boolean isMine;           // true nếu là tin nhắn của user hiện tại
    private LocalDateTime createdDate;
}