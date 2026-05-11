package com.example.zalo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CallSignalDto(
    val type: String, // call-offer, call-answer, call-reject, call-cancel, call-end, ice-candidate
    val chatId: String? = null,
    val targetUserId: String? = null,
    val fromUserId: String? = null,
    val fromUserName: String? = null,
    val fromUserAvatar: String? = null,
    val callType: String? = null, // VOICE, VIDEO
    val sdp: String? = null,
    val candidate: String? = null,
    val durationSec: Int? = null
)
