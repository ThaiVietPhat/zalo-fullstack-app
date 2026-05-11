package com.example.zalo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SmartReplyResponse(
    val suggestions: List<String>
)

@Serializable
data class SummarizeRequest(
    val lastNMessages: Int = 20
)

@Serializable
data class SummarizeResponse(
    val summary: String,
    val period: String? = null
)

@Serializable
data class AiChatRequest(
    val message: String
)

@Serializable
data class AiMessageDto(
    val id: String? = null,
    val role: String, // "USER" or "ASSISTANT"
    val content: String,
    val createdDate: String? = null
)

@Serializable
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
)
