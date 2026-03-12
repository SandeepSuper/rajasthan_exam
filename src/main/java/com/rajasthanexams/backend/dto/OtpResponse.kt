package com.rajasthanexams.backend.dto

data class OtpResponse(
    val message: String,
    val otp: String? = null // Included only for debug/mock purposes
)
