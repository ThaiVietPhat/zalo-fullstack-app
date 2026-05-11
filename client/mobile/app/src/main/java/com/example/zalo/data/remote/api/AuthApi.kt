package com.example.zalo.data.remote.api

import com.example.zalo.data.remote.dto.LoginRequest
import com.example.zalo.data.remote.dto.LoginResponse
import com.example.zalo.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Void>
}
