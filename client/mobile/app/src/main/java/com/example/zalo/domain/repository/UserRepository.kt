package com.example.zalo.domain.repository

import com.example.zalo.data.remote.dto.UserDto
import com.example.zalo.data.remote.dto.FriendRequestDto

interface UserRepository {
    suspend fun getMyProfile(): Result<UserDto>
    suspend fun getUserById(userId: String): Result<UserDto>
    suspend fun searchUsers(keyword: String): Result<List<UserDto>>
    suspend fun getContacts(): Result<List<UserDto>>
    suspend fun getPendingRequests(): Result<List<FriendRequestDto>>
    suspend fun sendFriendRequest(receiverId: String): Result<FriendRequestDto>
    suspend fun acceptFriendRequest(requestId: String): Result<FriendRequestDto>
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>
    suspend fun unfriend(friendId: String): Result<Unit>
}
