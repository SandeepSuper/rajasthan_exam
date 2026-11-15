package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.dto.AuthResponse
import com.rajasthanexams.backend.dto.OtpResponse
import com.rajasthanexams.backend.dto.ApiResponse
import com.rajasthanexams.backend.dto.UpdateProfileRequest
import org.springframework.transaction.annotation.Transactional
import com.rajasthanexams.backend.model.AppConfig
import com.rajasthanexams.backend.model.User
import com.rajasthanexams.backend.repository.AppConfigRepository
import com.rajasthanexams.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
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
    private val emailService: EmailService,
    private val appConfigRepository: AppConfigRepository
) {

    /**
     * Creates a JWT and stores it as the active session.
     * Web (header "X-Client: web") and Android get separate session slots,
     * so logging in on the website does not log the user out of the app.
     */
    private fun issueToken(subject: String): String {
        val request = (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
                as? org.springframework.web.context.request.ServletRequestAttributes)?.request
        val client = if (request?.getHeader("X-Client")?.lowercase() == "web") JwtService.CLIENT_WEB else null
        val token = jwtService.generateToken(subject, client)
        redisService.saveValue(JwtService.sessionKey(subject, client), token, 315360000)
        return token
    }

    @Value("\${app.google.client-id}")
    private lateinit var googleClientId: String

    private val passwordEncoder = BCryptPasswordEncoder()

    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build()
    }

    // ─── Email + Password Auth ─────────────────────────────────────

    /**
     * Step 1 of signup: create account with hashed password, send email OTP.
     * Returns a simple message; user must verify OTP before getting JWT.
     */
    fun registerWithEmail(name: String, email: String, password: String, referredByCode: String? = null): String {
        val normalizedEmail = email.trim().lowercase()

        // Check if email already registered and verified
        val existing = userRepository.findByEmail(normalizedEmail).orElse(null)
        if (existing != null && existing.emailVerified) {
            throw IllegalArgumentException("Email already registered. Please login.")
        }

        val passwordHash = passwordEncoder.encode(password)

        val user = existing ?: User(
            email = normalizedEmail,
            referCode = generateUniqueReferCode(normalizedEmail)
        )
        user.name = name
        user.passwordHash = passwordHash
        user.emailVerified = false
        userRepository.save(user)

        // Store referredByCode in Redis temporarily until OTP verified
        if (!referredByCode.isNullOrBlank()) {
            redisService.saveValue("ref:$normalizedEmail", referredByCode, 600)
        }

        sendEmailOtp(normalizedEmail)
        return "OTP sent to $normalizedEmail"
    }

    /**
     * Generate and send a 6-digit OTP to the given email address.
     */
    fun sendEmailOtp(email: String): OtpResponse {
        val normalizedEmail = email.trim().lowercase()
        val otp = String.format("%06d", Random().nextInt(1_000_000))
        
        println("   EMAIL OTP to $normalizedEmail: Your OTP is $otp   ")
        
        redisService.saveOtp("email:$normalizedEmail", otp)
        emailService.sendOtpEmail(normalizedEmail, otp)
        return OtpResponse("OTP sent to $normalizedEmail")
    }

    /**
     * Step 2 of signup: verify email OTP → mark email verified, apply referral, return JWT.
     */
    fun verifyEmailOtp(email: String, otp: String): AuthResponse {
        val normalizedEmail = email.trim().lowercase()
        val key = "email:$normalizedEmail"
        val storedOtp = redisService.getOtp(key) ?: throw IllegalArgumentException("OTP expired or not found. Please request a new one.")
        if (storedOtp != otp) throw IllegalArgumentException("Invalid OTP. Please try again.")
        redisService.deleteOtp(key)

        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { IllegalArgumentException("No account found for $normalizedEmail. Please sign up first.") }

        val isNewUser = !user.emailVerified
        user.emailVerified = true
        userRepository.save(user)

        // Apply referral for newly verified users
        if (isNewUser) {
            val referredByCode = redisService.getValue("ref:$normalizedEmail")
            if (!referredByCode.isNullOrBlank()) {
                applyReferral(user, referredByCode)
                redisService.deleteValue("ref:$normalizedEmail")
            }
        }

        val token = issueToken(normalizedEmail)
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

    /**
     * Login with email + password.
     */
    fun loginWithEmail(email: String, password: String): AuthResponse {
        val normalizedEmail = email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { IllegalArgumentException("No account found with this email.") }

        if (user.passwordHash.isNullOrBlank()) {
            throw IllegalArgumentException("This account uses Google Sign-In. Please continue with Google.")
        }
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw IllegalArgumentException("Incorrect password.")
        }
        if (!user.emailVerified) {
            throw IllegalArgumentException("Email not verified. Please check your inbox for the OTP.")
        }

        val token = issueToken(normalizedEmail)
        return AuthResponse(
            token = token,
            userId = user.id.toString(),
            name = user.name,
            email = user.email,
            profilePicture = user.profilePicture,
            isPremium = user.isPremium,
            isNewUser = false,
            coins = user.coins ?: 0,
            referCode = user.referCode
        )
    }

    // ─── Forgot Password ─────────────────────────────────────────────

    fun sendForgotPasswordOtp(email: String): ApiResponse {
        val normalizedEmail = email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { IllegalArgumentException("No account found with this email.") }

        if (user.passwordHash.isNullOrBlank()) {
            throw IllegalArgumentException("This account uses Google Sign-In. Please continue with Google.")
        }
        
        val otp = String.format("%06d", Random().nextInt(1_000_000))
        println("   FORGOT PASSWORD OTP to $normalizedEmail: Your OTP is $otp   ")
        redisService.saveOtp("forgot:$normalizedEmail", otp)
        emailService.sendOtpEmail(normalizedEmail, otp)

        return ApiResponse("Password reset OTP sent to $normalizedEmail", true)
    }

    fun resetPassword(email: String, otp: String, newPassword: String): ApiResponse {
        val normalizedEmail = email.trim().lowercase()
        val key = "forgot:$normalizedEmail"
        val storedOtp = redisService.getOtp(key) ?: throw IllegalArgumentException("OTP expired or not found. Please request a new one.")
        if (storedOtp != otp) throw IllegalArgumentException("Invalid OTP. Please try again.")
        
        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { IllegalArgumentException("No account found with this email.") }

        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        redisService.deleteOtp(key)

        return ApiResponse("Password successfully reset. You can now login.", true)
    }

    // ─── Google Auth (unchanged) ────────────────────────────────────

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
            val newUser = User(
                mobile = "",
                email = email,
                name = name,
                profilePicture = pictureUrl,
                emailVerified = true,   // Google users are already verified
                referCode = generateUniqueReferCode(email)
            )
            val createdUser = userRepository.save(newUser)
            if (!referredByCode.isNullOrBlank()) {
                applyReferral(createdUser, referredByCode)
            }
            createdUser
        }

        if (user.referCode.isNullOrBlank()) {
            user.referCode = generateUniqueReferCode(email)
            userRepository.save(user)
        }

        val token = issueToken(email)
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

    // ─── Legacy Mobile OTP (kept for backward compat) ──────────────

    fun sendOtp(mobile: String): OtpResponse {
        val otp = String.format("%04d", Random().nextInt(10000))
        redisService.saveOtp(mobile, otp)
        smsService.sendOtp(mobile, otp)
        return OtpResponse("OTP sent successfully", otp)
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

        if (user.referCode.isNullOrBlank()) {
            user.referCode = generateUniqueReferCode(mobile)
            userRepository.save(user)
        }

        val token = issueToken(mobile)
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

    // ─── Profile Update ─────────────────────────────────────────────

    /**
     * Updates the profile of the logged-in user ([subject] = JWT subject: email, or mobile for old OTP users).
     * Name, picture and (optionally) mobile can change freely. The email is the login identity, so it can
     * only be set when the account has none yet, and never to an address another account uses.
     */
    @Transactional
    fun updateProfile(subject: String, request: UpdateProfileRequest): User {
        val user = userRepository.findByEmail(subject)
            .or { userRepository.findByMobile(subject) }
            .orElseThrow { IllegalArgumentException("User not found") }
        if (!request.userId.isNullOrBlank() && request.userId != user.id.toString()) {
            throw IllegalArgumentException("Aap sirf apni profile badal sakte hain.")
        }

        val name = request.name.trim()
        if (name.isEmpty()) throw IllegalArgumentException("Naam khaali nahi ho sakta.")

        val email = request.email.trim().lowercase()
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        val currentEmail = user.email?.trim()?.lowercase()
        if (currentEmail.isNullOrEmpty()) {
            if (email.isNotEmpty()) {
                if (!email.matches(emailRegex.toRegex())) throw IllegalArgumentException("Invalid email format")
                val other = userRepository.findByEmail(email).orElse(null)
                if (other != null && other.id != user.id) throw IllegalArgumentException("Yeh email kisi aur account mein use ho raha hai.")
                user.email = email
            }
        } else if (email.isNotEmpty() && email != currentEmail) {
            throw IllegalArgumentException("Email badla nahi ja sakta.")
        }

        if (request.updateMobile) {
            val mobile = request.mobile?.trim().orEmpty()
            if (mobile.isNotEmpty() && !mobile.matches(Regex("^[6-9]\\d{9}$"))) {
                throw IllegalArgumentException("Sahi 10-digit mobile number daalein.")
            }
            if (mobile.isNotEmpty()) {
                val other = userRepository.findByMobile(mobile).orElse(null)
                if (other != null && other.id != user.id) throw IllegalArgumentException("Yeh mobile number kisi aur account mein use ho raha hai.")
            }
            // Old OTP users log in with their mobile; changing it would lock them out.
            if (user.mobile != null && subject == user.mobile && mobile != user.mobile) {
                throw IllegalArgumentException("Login wala mobile number badla nahi ja sakta.")
            }
            user.mobile = mobile.ifEmpty { null }
        }

        user.name = name
        user.profilePicture = request.profilePicture
        val referredByCode = request.referredByCode

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

    // ─── Helpers ────────────────────────────────────────────────────

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

    private fun generateUniqueReferCode(identifier: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..6).map { chars.random() }.joinToString("")
        return "RAJ-$suffix"
    }
}
