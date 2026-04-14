package com.example.backend.group.dto;

import com.example.backend.messaging.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupDto {
    private UUID id;
    private String name;
    private String description;
    private String avatarUrl;
    private UUID createdById;
    private int memberCount;
    private List<GroupMemberDto> members;

    // Preview tin nhắn cuối
    private String lastMessage;
    private MessageType lastMessageType;
    private LocalDateTime lastMessageTime;
    private String lastMessageSenderName;

    // Quyền của user hiện tại trong nhóm
    @JsonProperty("isAdmin")
    private boolean isAdmin;

    private List<GroupMessageDto> pinnedMessages;
}