package com.example.zalo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val createdById: String,
    val memberCount: Int,
    val members: List<GroupMemberDto>? = null,
    val lastMessage: String? = null,
    val lastMessageType: String? = null,
    val lastMessageTime: String? = null,
    val lastMessageSenderName: String? = null,
    val isAdmin: Boolean = false,
    val unreadCount: Int = 0
)

@Serializable
data class GroupMemberDto(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val admin: Boolean = false,
    val online: Boolean = false,
    val lastSeenText: String? = null
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val memberIds: List<String>
)

@Serializable
data class SendGroupMessageRequest(
    val content: String,
    val type: String = "TEXT"
)
