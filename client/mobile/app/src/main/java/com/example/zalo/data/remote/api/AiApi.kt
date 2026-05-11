package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AiApi {
    @POST("chat/{chatId}/ai/smart-reply")
    suspend fun getChatSmartReply(@Path("chatId") chatId: String): SmartReplyResponse

    @POST("group/{groupId}/ai/smart-reply")
    suspend fun getGroupSmartReply(@Path("groupId") groupId: String): SmartReplyResponse

    @POST("chat/{chatId}/ai/summarize")
    suspend fun summarizeChat(
        @Path("chatId") chatId: String,
        @Body request: SummarizeRequest
    ): SummarizeResponse

    @POST("group/{groupId}/ai/summarize")
    suspend fun summarizeGroup(
        @Path("groupId") groupId: String,
        @Body request: SummarizeRequest
    ): SummarizeResponse

    // AI Direct Chat
    @POST("ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): Response<AiMessageDto>

    @GET("ai/history")
    suspend fun getAiHistory(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<AiMessageDto>>

    @DELETE("ai/history")
    suspend fun clearAiHistory(): Response<Void>
}
