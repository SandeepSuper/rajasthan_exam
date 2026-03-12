package com.rajasthanexams.backend.service

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dev", "prod", "default") // Active by default or when 'dev' profile is active
class MockSmsService : SmsService {
    private val logger = LoggerFactory.getLogger(MockSmsService::class.java)

    override fun sendOtp(mobile: String, otp: String): Boolean {
        logger.info("--------------------------------------------------")
        logger.info("   MOCK SMS to $mobile: Your OTP is $otp   ")
        logger.info("--------------------------------------------------")
        println("--------------------------------------------------")
        println("   MOCK SMS to $mobile: Your OTP is $otp   ") // Print to console for easy visibility
        println("--------------------------------------------------")
        return true
    }
}
