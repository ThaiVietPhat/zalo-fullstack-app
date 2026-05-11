package com.example.zalo.data.repository

import com.example.zalo.data.remote.api.AdminApi
import com.example.zalo.data.remote.dto.*
import com.example.zalo.domain.repository.AdminRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val adminApi: AdminApi
) : AdminRepository {

    override suspend fun getStats(): Result<AdminStatsDto> {
        return try {
            val response = adminApi.getStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUsers(page: Int): Result<List<UserDto>> {
        return try {
            val response = adminApi.getAdminUsers(page, 50)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroups(page: Int): Result<List<GroupDto>> {
        return try {
            val response = adminApi.getAdminGroups(page, 50)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuditLogs(page: Int): Result<List<AuditLogDto>> {
        return try {
            val response = adminApi.getAuditLogs(page, 50)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun banUser(userId: String, reason: String, days: Int?): Result<Unit> {
        return try {
            val response = adminApi.banUser(userId, BanUserRequest(reason, days))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun unbanUser(userId: String): Result<Unit> {
        return try {
            val response = adminApi.unbanUser(userId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun promoteUser(userId: String): Result<Unit> {
        return try {
            val response = adminApi.promoteUser(userId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun demoteUser(userId: String): Result<Unit> {
        return try {
            val response = adminApi.demoteUser(userId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            val response = adminApi.deleteUser(userId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            val response = adminApi.deleteAdminGroup(groupId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }
}
