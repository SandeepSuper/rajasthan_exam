package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.dto.AuthResponse
import com.rajasthanexams.backend.model.AppConfig
import com.rajasthanexams.backend.model.User
import com.rajasthanexams.backend.repository.AppConfigRepository
import com.rajasthanexams.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import java.util.Collections
import java.util.Random

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val redisService: RedisService,
    private val jwtService: JwtService,
    private val smsService: SmsService,
    private val appConfigRepository: AppConfigRepository
) {

    @Value("\${app.google.client-id}")
    private lateinit var googleClientId: String

    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build()
    }

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

    fun loginWithGoogle(idTokenString: String, referredByCode: String? = null): AuthResponse {
        val idToken: GoogleIdToken = verifier.verify(idTokenString) 
            ?: throw IllegalArgumentException("Invalid Google ID token")

        val payload: GoogleIdToken.Payload = idToken.payload
        val email = payload.email ?: throw IllegalArgumentException("Google account has no email")
        val name = payload["name"] as String?
        val pictureUrl = payload["picture"] as String?

        var isNewUser = false
        val user = userRepository.findByEmail(email).orElseGet {
            isNewUser = true
            
            // Generate a random dummy mobile for now just in case, or leave null if DB allows.
            // Since we made mobile nullable, we can use an empty mobile or a placeholder.
            // But User entity needs mobile to be unique. A null mobile violates unique constraint if multiple users have null?
            // Actually PostgreSQL allows multiple nulls in a UNIQUE constraint.
            val newUser = User(
                mobile = "", // temporary placeholder if required, but we made it nullable so let's do null
                email = email,
                name = name,
                profilePicture = pictureUrl,
                referCode = generateUniqueReferCode(email)
            )
            // PostgreSQL unique constraints ignore nulls, so setting mobile to null is perfect.
            // Wait, we still have `val mobile: String? = null` in User.kt, so we can omit it.
            val createdUser = userRepository.save(newUser)
            
            // Handle referral
            if (!referredByCode.isNullOrBlank()) {
                 applyReferral(createdUser, referredByCode)
            }
            createdUser
        }

        // Generate referral code for existing users who don't have one yet
        if (user.referCode.isNullOrBlank()) {
            user.referCode = generateUniqueReferCode(email)
            userRepository.save(user)
        }

        val token = jwtService.generateToken(email) // Using email as subject for Google users
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

    private fun applyReferral(user: User, referredByCode: String) {
         val config = appConfigRepository.findById(1L).orElseGet {
            appConfigRepository.save(AppConfig())
        }

        val referrer = userRepository.findByReferCode(referredByCode).orElse(null)
        if (referrer != null && referrer.id != user.id) {
            referrer.coins = (referrer.coins ?: 0) + config.referrerCoinReward
            referrer.referredCount = (referrer.referredCount ?: 0) + 1
            referrer.historicalReferralCoinsEarned = (referrer.historicalReferralCoinsEarned ?: 0) + config.referrerCoinReward
            userRepository.save(referrer)
            
            user.coins = (user.coins ?: 0) + config.refereeCoinReward
            user.referredBy = referredByCode
            user.referrerRewardAmount = config.referrerCoinReward
            userRepository.save(user)
        }
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
            val config = appConfigRepository.findById(1L).orElseGet {
                appConfigRepository.save(AppConfig())
            }

            val referrer = userRepository.findByReferCode(referredByCode).orElse(null)
            if (referrer != null && referrer.id != user.id) {
                referrer.coins = (referrer.coins ?: 0) + config.referrerCoinReward
                referrer.referredCount = (referrer.referredCount ?: 0) + 1
                referrer.historicalReferralCoinsEarned = (referrer.historicalReferralCoinsEarned ?: 0) + config.referrerCoinReward
                userRepository.save(referrer)
                
                user.coins = (user.coins ?: 0) + config.refereeCoinReward
                user.referredBy = referredByCode
                user.referrerRewardAmount = config.referrerCoinReward
            }
        }

        return userRepository.save(user)
    }

    /** Generate a short unique referral code like "RAJ-9X4K2A" */
    private fun generateUniqueReferCode(identifier: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..6).map { chars.random() }.joinToString("")
        return "RAJ-$suffix"
    }
}
