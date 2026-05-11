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

    @Multipart
    @POST("group/{groupId}/upload-media")
    suspend fun uploadGroupMedia(
        @Path("groupId") groupId: String,
        @Part file: MultipartBody.Part
    ): Response<GroupMessageDto>
}
