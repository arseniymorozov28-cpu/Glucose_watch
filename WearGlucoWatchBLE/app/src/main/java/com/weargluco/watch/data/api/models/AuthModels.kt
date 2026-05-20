package com.weargluco.watch.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val status: Int,
    val data: LoginData? = null,
    val error: ErrorInfo? = null
)

@Serializable
data class ErrorInfo(
    val message: String? = null
)

@Serializable
data class LoginData(
    val user: UserInfo,
    val authTicket: AuthTicket
)

@Serializable
data class UserInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val country: String
)

@Serializable
data class AuthTicket(
    val token: String,
    val expires: Long,
    val duration: Long
)
