package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.SmartReplyResponse
import com.example.zalo.data.remote.dto.SummarizeRequest
import com.example.zalo.data.remote.dto.SummarizeResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

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
}
