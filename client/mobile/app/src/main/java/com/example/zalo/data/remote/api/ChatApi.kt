package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.ChatDto
import com.example.zalo.data.remote.dto.MessageDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {
    @GET("chat")
    suspend fun getAllChats(): Response<List<ChatDto>>

    @GET("chat/{chatId}")
    suspend fun getChatById(@Path("chatId") chatId: String): Response<ChatDto>

    @POST("chat/start/{otherUserId}")
    suspend fun startChat(@Path("otherUserId") otherUserId: String): Response<ChatDto>
}

interface MessageApi {
    @GET("message/chat/{chatId}")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<List<MessageDto>>

    @POST("message")
    suspend fun sendMessage(@Body message: MessageDto): Response<MessageDto>

    @PATCH("message/seen/{chatId}")
    suspend fun markAsSeen(@Path("chatId") chatId: String): Response<Void>

    @Multipart
    @POST("message/upload-media/{chatId}")
    suspend fun uploadMedia(
        @Path("chatId") chatId: String,
        @Part file: MultipartBody.Part
    ): Response<MessageDto>
}
