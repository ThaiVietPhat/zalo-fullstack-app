package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    @GET("user/me")
    suspend fun getMyProfile(): Response<UserDto>

    @GET("user/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserDto>

    @GET("user/search")
    suspend fun searchUsers(@Query("keyword") keyword: String): Response<List<UserDto>>
}
