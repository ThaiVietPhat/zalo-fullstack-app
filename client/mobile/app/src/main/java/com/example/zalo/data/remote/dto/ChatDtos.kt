package com.example.zalo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatDto(
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val chatName: String? = null,
    val lastMessage: String? = null,
    val lastMessageType: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Long = 0,
    val recipientOnline: Boolean = false,
    val recipientLastSeenText: String? = null,
    val avatarUrl: String? = null,
    val recipientId: String? = null,
    val recipientEmail: String? = null,
    val blockStatus: String? = null
)
