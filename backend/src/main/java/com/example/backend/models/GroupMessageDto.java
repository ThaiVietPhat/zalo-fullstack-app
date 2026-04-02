package com.example.backend.models;

import com.example.backend.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupMessageDto {
    private UUID id;
    private String content;
    private MessageType type;
    private UUID groupId;
    private UUID senderId;
    private String senderName;
    @JsonProperty("isMine")
    private boolean isMine;
    private LocalDateTime createdDate;
    private boolean deleted;
    private List<ReactionDto> reactions;
}
