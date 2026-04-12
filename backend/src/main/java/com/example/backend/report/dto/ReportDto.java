package com.example.backend.report.dto;

import com.example.backend.report.entity.ReportStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportDto {
    private Long id;
    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;
    private UUID reportedId;
    private String reportedName;
    private String reportedEmail;
    private String reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolution;
    private boolean reportedBanned;
}
