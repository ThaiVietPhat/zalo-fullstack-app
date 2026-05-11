package com.example.zalo.domain.repository

import com.example.zalo.data.remote.dto.LoginRequest
import com.example.zalo.data.remote.dto.LoginResponse
import com.example.zalo.data.remote.dto.RegisterRequest

interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<LoginResponse>
    suspend fun register(request: RegisterRequest): Result<Unit>
    fun logout()
    fun isLoggedIn(): Boolean
}
