package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.*
import com.rajasthanexams.backend.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Email+Password login and Google OAuth")
class AuthController(
    private val authService: AuthService
) {

    // ─── Email + Password Auth ─────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register with name, email, password. Sends OTP to email for verification.")
    fun register(@RequestBody request: EmailRegisterRequest): ResponseEntity<ApiResponse> {
        return try {
            val message = authService.registerWithEmail(
                name = request.name,
                email = request.email,
                password = request.password,
                referredByCode = request.referredByCode
            )
            ResponseEntity.ok(ApiResponse(message, true))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(e.message ?: "Registration failed", false))
        }
    }

    @PostMapping("/send-email-otp")
    @Operation(summary = "Send Email OTP", description = "Generates and sends a 6-digit OTP to the provided email address.")
    fun sendEmailOtp(@RequestBody request: SendEmailOtpRequest): ResponseEntity<ApiResponse> {
        return try {
            authService.sendEmailOtp(request.email)
            ResponseEntity.ok(ApiResponse("OTP sent to ${request.email}", true))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(e.message ?: "Failed to send OTP", false))
        }
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify Email OTP", description = "Verifies the email OTP. Returns JWT token on success.")
    fun verifyEmailOtp(@RequestBody request: VerifyEmailOtpRequest): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.verifyEmailOtp(request.email, request.otp)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login with email and password. Returns JWT token.")
    fun login(@RequestBody request: EmailLoginRequest): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.loginWithEmail(request.email, request.password)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    // ─── Google Auth (unchanged) ────────────────────────────────────

    @PostMapping("/google")
    @Operation(summary = "Google Login", description = "Verifies Google ID token and returns a JWT token.")
    fun googleLogin(@RequestBody request: GoogleLoginRequest): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.loginWithGoogle(request.idToken, request.referredByCode)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    // ─── Profile Update ─────────────────────────────────────────────

    @PostMapping("/update-profile")
    @Operation(summary = "Update Profile", description = "Updates user name, email, and profile picture.")
    fun updateProfile(@RequestBody request: UpdateProfileRequest): ResponseEntity<ApiResponse> {
        return try {
            authService.updateProfile(request.userId, request.name, request.email, request.profilePicture, request.referredByCode)
            ResponseEntity.ok(ApiResponse("Profile updated successfully", true))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(e.message ?: "Update failed", false))
        }
    }

    // ─── Legacy Mobile OTP (deprecated, kept for backward compat) ──

    @PostMapping("/send-otp")
    @Operation(summary = "[Deprecated] Send Mobile OTP", description = "Legacy endpoint. No longer used.")
    fun sendOtp(@RequestBody request: OtpRequest): ResponseEntity<OtpResponse> {
        val result = authService.sendOtp(request.mobile)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "[Deprecated] Verify Mobile OTP", description = "Legacy endpoint. No longer used.")
    fun verifyOtp(@RequestBody request: VerifyOtpRequest): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.verifyOtp(request.mobile, request.otp)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
}
