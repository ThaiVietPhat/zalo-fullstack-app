package com.example.zalo.data.repository

import com.example.zalo.data.remote.api.UserApi
import com.example.zalo.data.remote.api.FriendApi
import com.example.zalo.data.remote.dto.UserDto
import com.example.zalo.data.remote.dto.FriendRequestDto
import com.example.zalo.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val friendApi: FriendApi
) : UserRepository {

    override suspend fun getMyProfile(): Result<UserDto> {
        return try {
            val response = userApi.getMyProfile()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<UserDto> {
        return try {
            val response = userApi.getUserById(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(keyword: String): Result<List<UserDto>> {
        return try {
            val response = userApi.searchUsers(keyword)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContacts(): Result<List<UserDto>> {
        return try {
            val response = friendApi.getContacts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingRequests(): Result<List<FriendRequestDto>> {
        return try {
            val response = friendApi.getPendingRequests()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendFriendRequest(receiverId: String): Result<FriendRequestDto> {
        return try {
            val response = friendApi.sendFriendRequest(receiverId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<FriendRequestDto> {
        return try {
            val response = friendApi.acceptFriendRequest(requestId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            val response = friendApi.rejectFriendRequest(requestId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfriend(friendId: String): Result<Unit> {
        return try {
            val response = friendApi.unfriend(friendId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
