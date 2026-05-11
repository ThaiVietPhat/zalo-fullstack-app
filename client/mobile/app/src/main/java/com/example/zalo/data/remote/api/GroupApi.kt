package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface GroupApi {
    @GET("group")
    suspend fun getMyGroups(): Response<List<GroupDto>>

    @POST("group")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<GroupDto>

    @GET("group/{groupId}")
    suspend fun getGroup(@Path("groupId") groupId: String): Response<GroupDto>

    @GET("group/{groupId}/messages")
    suspend fun getGroupMessages(
        @Path("groupId") groupId: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 30
    ): Response<List<GroupMessageDto>>

    @POST("group/{groupId}/messages")
    suspend fun sendGroupMessage(
        @Path("groupId") groupId: String,
        @Body request: SendGroupMessageRequest
    ): Response<GroupMessageDto>

    @POST("group/{groupId}/members")
    suspend fun addMembers(
        @Path("groupId") groupId: String,
        @Body userIds: List<String>
    ): Response<GroupDto>

    @DELETE("group/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: String): Response<Void>

    @PUT("group/{groupId}")
    suspend fun updateGroup(
        @Path("groupId") groupId: String,
        @Body request: Map<String, String>
    ): Response<GroupDto>

    @DELETE("group/{groupId}/members/{userId}")
    suspend fun removeMember(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String
    ): Response<Void>

    @PATCH("group/{groupId}/members/{userId}/set-admin")
    suspend fun setMemberAsAdmin(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String
    ): Response<Void>

    @DELETE("group/{groupId}/dissolve")
    suspend fun dissolveGroup(@Path("groupId") groupId: String): Response<Void>

    @POST("group/{groupId}/messages/{messageId}/pin")
    suspend fun pinMessage(
        @Path("groupId") groupId: String,
        @Path("messageId") messageId: String
    ): Response<Void>

    @DELETE("group/{groupId}/messages/{messageId}/pin")
    suspend fun unpinMessage(
        @Path("groupId") groupId: String,
        @Path("messageId") messageId: String
    ): Response<Void>

    @GET("group/{groupId}/pinned-messages")
    suspend fun getPinnedMessages(@Path("groupId") groupId: String): Response<List<GroupMessageDto>>

    @POST("group/{groupId}/join-requests")
    suspend fun createJoinRequest(
        @Path("groupId") groupId: String,
        @Body userIds: List<String>
    ): Response<Void>

    @GET("group/{groupId}/join-requests")
    suspend fun getJoinRequests(@Path("groupId") groupId: String): Response<List<GroupJoinRequestDto>>

    @PUT("group/{groupId}/join-requests/{requestId}/approve")
    suspend fun approveJoinRequest(
        @Path("groupId") groupId: String,
        @Path("requestId") requestId: String
    ): Response<Void>

    @PUT("group/{groupId}/join-requests/{requestId}/reject")
    suspend fun rejectJoinRequest(
        @Path("groupId") groupId: String,
        @Path("requestId") requestId: String
    ): Response<Void>

    @POST("group-message/{messageId}/reactions")
    suspend fun toggleGroupReaction(
        @Path("messageId") messageId: String,
        @Query("emoji") emoji: String
    ): Response<Void>

    @DELETE("group-message/{messageId}/reactions")
    suspend fun deleteGroupReaction(@Path("messageId") messageId: String): Response<Void>

    @Multipart
    @POST("group/{groupId}/upload-media")
    suspend fun uploadGroupMedia(
        @Path("groupId") groupId: String,
        @Part file: MultipartBody.Part
    ): Response<GroupMessageDto>
}
