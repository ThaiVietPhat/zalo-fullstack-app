package com.example.zalo.domain.repository

import com.example.zalo.data.remote.dto.*

interface AdminRepository {
    suspend fun getStats(): Result<AdminStatsDto>
    suspend fun getUsers(page: Int): Result<List<UserDto>>
    suspend fun getGroups(page: Int): Result<List<GroupDto>>
    suspend fun getAuditLogs(page: Int): Result<List<AuditLogDto>>
    
    suspend fun banUser(userId: String, reason: String, days: Int? = null): Result<Unit>
    suspend fun unbanUser(userId: String): Result<Unit>
    suspend fun promoteUser(userId: String): Result<Unit>
    suspend fun demoteUser(userId: String): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    
    suspend fun deleteGroup(groupId: String): Result<Unit>
}
