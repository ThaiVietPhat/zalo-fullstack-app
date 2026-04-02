package com.example.backend.models;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReactionDto {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private String emoji;
    private LocalDateTime createdDate;
}
