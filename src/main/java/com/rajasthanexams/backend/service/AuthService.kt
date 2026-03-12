package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.dto.AuthResponse
import com.rajasthanexams.backend.model.User
import com.rajasthanexams.backend.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.Random

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val redisService: RedisService,
    private val jwtService: JwtService,
    private val smsService: SmsService
) {

    fun sendOtp(mobile: String): com.rajasthanexams.backend.dto.OtpResponse {
        val otp = String.format("%04d", Random().nextInt(10000))
        redisService.saveOtp(mobile, otp)
        smsService.sendOtp(mobile, otp)
        return com.rajasthanexams.backend.dto.OtpResponse("OTP sent successfully", otp)
    }

    fun verifyOtp(mobile: String, otp: String): AuthResponse {
        val storedOtp = redisService.getOtp(mobile) ?: throw IllegalArgumentException("OTP expired or invalid")
        if (storedOtp != otp) throw IllegalArgumentException("Invalid OTP")
        redisService.deleteOtp(mobile)

        var isNewUser = false
        val user = userRepository.findByMobile(mobile).orElseGet {
            isNewUser = true
            val newUser = User(
                mobile = mobile,
                referCode = generateUniqueReferCode(mobile)
            )
            userRepository.save(newUser)
        }

        if (user.name.isNullOrBlank()) isNewUser = true

        // Generate referral code for existing users who don't have one yet
        if (user.referCode.isNullOrBlank()) {
            user.referCode = generateUniqueReferCode(mobile)
            userRepository.save(user)
        }

        val token = jwtService.generateToken(mobile)
        return AuthResponse(
            token = token,
            userId = user.id.toString(),
            name = user.name,
            email = user.email,
            profilePicture = user.profilePicture,
            isPremium = user.isPremium,
            isNewUser = isNewUser,
            coins = user.coins ?: 0,
            referCode = user.referCode
        )
    }

    fun updateProfile(userId: String, name: String, email: String, profilePicture: String?, referredByCode: String? = null): User {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        if (!email.matches(emailRegex.toRegex())) throw IllegalArgumentException("Invalid email format")

        val user = userRepository.findById(java.util.UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("User not found") }

        user.name = name
        user.email = email
        user.profilePicture = profilePicture

        // Apply referral reward only once (when user has no referredBy yet)
        if (!referredByCode.isNullOrBlank() && user.referredBy.isNullOrBlank()) {
            val referrer = userRepository.findByReferCode(referredByCode).orElse(null)
            if (referrer != null && referrer.id != user.id) {
                referrer.coins = (referrer.coins ?: 0) + 50
                referrer.referredCount = (referrer.referredCount ?: 0) + 1
                userRepository.save(referrer)
                user.coins = (user.coins ?: 0) + 20
                user.referredBy = referredByCode
            }
        }

        return userRepository.save(user)
    }

    /** Generate a short unique referral code like "RAJ-9X4K2A" */
    private fun generateUniqueReferCode(mobile: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..6).map { chars.random() }.joinToString("")
        return "RAJ-$suffix"
    }
}
