package com.example.backend.models;

import com.example.backend.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupMemberDto {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private boolean admin;
    private boolean online;
    private String lastSeenText;
}