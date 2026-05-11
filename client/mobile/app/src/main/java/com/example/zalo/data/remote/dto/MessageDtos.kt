package com.example.zalo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String? = null,
    val chatId: String? = null,
    val content: String? = null,
    val state: String? = null,
    val type: String? = null,
    val createdAt: String? = null,
    val senderId: String? = null,
    val receiverId: String? = null,
    val mediaUrl: String? = null,
    val fileName: String? = null,
    val deleted: Boolean = false,
    val reactions: List<ReactionDto> = emptyList(),
    val senderName: String? = null
)

@Serializable
data class GroupMessageDto(
    val id: String? = null,
    val content: String? = null,
    val mediaUrl: String? = null,
    val fileName: String? = null,
    val type: String? = null,
    val groupId: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val isMine: Boolean = false,
    val createdDate: String? = null,
    val deleted: Boolean = false,
    val pinned: Boolean = false,
    val hiddenForMe: Boolean = false,
    val reactions: List<ReactionDto> = emptyList()
)

@Serializable
data class ReactionDto(
    val id: String? = null,
    val userId: String? = null,
    val userFullName: String? = null,
    val emoji: String? = null,
    val createdDate: String? = null,
    val messageId: String? = null,
    val groupMessageId: String? = null
)

@Serializable
data class TypingPayload(
    val userId: String,
    val typing: Boolean
)
