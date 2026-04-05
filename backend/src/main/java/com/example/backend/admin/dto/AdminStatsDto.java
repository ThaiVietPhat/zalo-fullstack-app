package com.example.backend.admin.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminStatsDto {
    private long totalUsers;
    private long totalMessages;
    private long totalGroups;
    private long onlineUsers;
    private long bannedUsers;
    private List<DailyCountDto> dailyMessageCounts;
    private List<TopUserDto> topActiveUsers;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DailyCountDto {
        private String date;
        private long count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopUserDto {
        private UUID userId;
        private String fullName;
        private long messageCount;
    }
}
