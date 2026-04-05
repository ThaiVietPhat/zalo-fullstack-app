package com.example.backend.admin.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminUserDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean banned;
    private boolean online;
    private LocalDateTime lastSeen;
    private LocalDateTime createdDate;
}
