package com.example.backend.models;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminGroupDto {
    private UUID id;
    private String name;
    private String description;
    private int memberCount;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdDate;
}
