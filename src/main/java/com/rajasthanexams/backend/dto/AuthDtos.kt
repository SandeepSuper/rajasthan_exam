package com.rajasthanexams.backend.dto

data class OtpRequest(
    val mobile: String
)

data class VerifyOtpRequest(
    val mobile: String,
    val otp: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val name: String?,
    val email: String?,
    val profilePicture: String?,
    val isPremium: Boolean,
    val isNewUser: Boolean,
    val coins: Int = 0,
    val referCode: String? = null
)

data class UpdateProfileRequest(
    val userId: String,
    val name: String,
    val email: String,
    val profilePicture: String? = null,
    val referredByCode: String? = null
)

data class ApiResponse(
    val message: String,
    val success: Boolean = true
)

data class UserProfileResponse(
    val id: String,
    val name: String?,
    val email: String?,
    val profilePicture: String?,
    val coins: Int,
    val referCode: String?,
    val isPremium: Boolean
)
