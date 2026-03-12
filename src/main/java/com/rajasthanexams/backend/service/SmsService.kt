package com.rajasthanexams.backend.service

interface SmsService {
    fun sendOtp(mobile: String, otp: String): Boolean
}
