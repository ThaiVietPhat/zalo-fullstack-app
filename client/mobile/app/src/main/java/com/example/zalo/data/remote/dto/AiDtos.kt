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
