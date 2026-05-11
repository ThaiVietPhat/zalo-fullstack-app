package com.example.zalo.data.remote.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val online: Boolean
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)
