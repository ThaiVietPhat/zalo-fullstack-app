package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.FriendRequestDto
import com.example.zalo.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.*

interface FriendApi {
    @POST("friend-request/send/{receiverId}")
    suspend fun sendFriendRequest(@Path("receiverId") receiverId: String): Response<FriendRequestDto>

    @POST("friend-request/{requestId}/accept")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: String): Response<FriendRequestDto>

    @POST("friend-request/{requestId}/reject")
    suspend fun rejectFriendRequest(@Path("requestId") requestId: String): Response<Void>

    @GET("friend-request/pending")
    suspend fun getPendingRequests(): Response<List<FriendRequestDto>>

    @GET("friend-request/contacts")
    suspend fun getContacts(): Response<List<UserDto>>

    @DELETE("friend-request/unfriend/{friendId}")
    suspend fun unfriend(@Path("friendId") friendId: String): Response<Void>
}
