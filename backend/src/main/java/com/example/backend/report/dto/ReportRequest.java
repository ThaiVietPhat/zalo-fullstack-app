package com.example.backend.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ReportRequest {
    @NotBlank(message = "Lý do tố cáo không được để trống")
    private String reason;
    private String description;
}
