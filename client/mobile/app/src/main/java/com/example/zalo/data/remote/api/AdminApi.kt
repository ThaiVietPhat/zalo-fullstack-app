package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApi {
    @GET("admin/users")
    suspend fun getAdminUsers(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<UserDto>>

    @PATCH("admin/users/{userId}/ban")
    suspend fun banUser(
        @Path("userId") userId: String,
        @Body request: BanUserRequest
    ): Response<Void>

    @PATCH("admin/users/{userId}/unban")
    suspend fun unbanUser(@Path("userId") userId: String): Response<Void>

    @DELETE("admin/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<Void>

    @PATCH("admin/users/{userId}/promote")
    suspend fun promoteUser(@Path("userId") userId: String): Response<Void>

    @PATCH("admin/users/{userId}/demote")
    suspend fun demoteUser(@Path("userId") userId: String): Response<Void>

    @GET("admin/groups")
    suspend fun getAdminGroups(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<GroupDto>>

    @DELETE("admin/groups/{groupId}")
    suspend fun deleteAdminGroup(@Path("groupId") groupId: String): Response<Void>

    @GET("admin/stats")
    suspend fun getStats(): Response<AdminStatsDto>

    @GET("admin/audit-logs")
    suspend fun getAuditLogs(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<AuditLogDto>>
}
