package com.example.zalo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val lastSeen: String? = null,
    val online: Boolean = false,
    val lastSeenText: String? = null,
    val avatarUrl: String? = null,
    val friendshipStatus: String? = "NONE",
    val blockStatus: String? = "NONE",
    val role: String? = "USER"
)

@Serializable
data class FriendRequestDto(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val status: String,
    val createdDate: String
)
