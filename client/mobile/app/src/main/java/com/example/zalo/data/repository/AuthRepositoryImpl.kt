package com.example.zalo.data.repository

import com.example.zalo.data.local.TokenManager
import com.example.zalo.data.remote.api.AuthApi
import com.example.zalo.data.remote.dto.LoginRequest
import com.example.zalo.data.remote.dto.LoginResponse
import com.example.zalo.data.remote.dto.RegisterRequest
import com.example.zalo.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = api.login(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveRefreshToken(body.refreshToken)
                tokenManager.saveUserId(body.userId)
                Result.success(body)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<Unit> {
        return try {
            val response = api.register(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        tokenManager.clear()
    }

    override fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}
