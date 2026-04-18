package com.example.backend.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SummarizeRequest {
    /** Tóm tắt từ thời điểm này (ISO LocalDateTime) */
    private LocalDateTime since;
}
