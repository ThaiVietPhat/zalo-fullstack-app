package com.example.zalo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminStatsDto(
    val totalUsers: Long = 0,
    val totalMessages: Long = 0,
    val totalGroups: Long = 0,
    val totalChats: Long = 0,
    val onlineUsers: Long = 0,
    val bannedUsers: Long = 0,
    val verifiedUsers: Long = 0,
    val dailyMessageCounts: List<DailyCountDto> = emptyList(),
    val dailyNewUsers: List<DailyCountDto> = emptyList(),
    val dailyNewGroups: List<DailyCountDto> = emptyList(),
    val topActiveUsers: List<TopUserDto> = emptyList()
)

@Serializable
data class DailyCountDto(
    val date: String,
    val count: Long
)

@Serializable
data class TopUserDto(
    val userId: String,
    val fullName: String,
    val messageCount: Long
)

@Serializable
data class AuditLogDto(
    val id: String,
    val adminId: String,
    val adminName: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val createdDate: String
)

@Serializable
data class BanUserRequest(
    val reason: String,
    val days: Int? = null
)
