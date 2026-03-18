package com.rajasthanexams.backend.dto

data class GoogleLoginRequest(
    val idToken: String,
    val referredByCode: String? = null
)

// ── Email+Password Auth ─────────────────────────────────────────

data class EmailRegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val referredByCode: String? = null
)

data class EmailLoginRequest(
    val email: String,
    val password: String
)

data class SendEmailOtpRequest(
    val email: String
)

data class VerifyEmailOtpRequest(
    val email: String,
    val otp: String
)

// ── Legacy Mobile OTP (kept for backward compat) ────────────────

data class OtpRequest(
    val mobile: String
)

data class VerifyOtpRequest(
    val mobile: String,
    val otp: String
)

// ── Shared Responses ────────────────────────────────────────────
// Note: OtpResponse is declared in OtpResponse.kt — not duplicated here

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
    val mobile: String?,
    val profilePicture: String?,
    val coins: Int,
    val referCode: String?,
    val isPremium: Boolean
)
