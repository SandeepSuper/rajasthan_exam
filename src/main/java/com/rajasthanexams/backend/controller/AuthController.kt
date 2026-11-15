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
    private val authService: AuthService,
    private val jwtService: com.rajasthanexams.backend.service.JwtService,
    private val redisService: com.rajasthanexams.backend.service.RedisService
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
    fun verifyEmailOtp(
        @RequestBody request: VerifyEmailOtpRequest,
        httpRequest: jakarta.servlet.http.HttpServletRequest,
        httpResponse: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.verifyEmailOtp(request.email, request.otp)
            withSession(response, httpRequest, httpResponse)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login with email and password. Returns JWT token.")
    fun login(
        @RequestBody request: EmailLoginRequest,
        httpRequest: jakarta.servlet.http.HttpServletRequest,
        httpResponse: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.loginWithEmail(request.email, request.password)
            withSession(response, httpRequest, httpResponse)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    // ─── Forgot Password ────────────────────────────────────────────

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Sends a 6-digit OTP to the provided email for password reset.")
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<ApiResponse> {
        return try {
            val response = authService.sendForgotPasswordOtp(request.email)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(e.message ?: "Failed to send OTP", false))
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Verifies the OTP and resets the user's password.")
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<ApiResponse> {
        return try {
            val response = authService.resetPassword(request.email, request.otp, request.newPassword)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(e.message ?: "Failed to reset password", false))
        }
    }

    // ─── Google Auth (unchanged) ────────────────────────────────────

    @PostMapping("/google")
    @Operation(summary = "Google Login", description = "Verifies Google ID token and returns a JWT token.")
    fun googleLogin(
        @RequestBody request: GoogleLoginRequest,
        httpRequest: jakarta.servlet.http.HttpServletRequest,
        httpResponse: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.loginWithGoogle(request.idToken, request.referredByCode)
            withSession(response, httpRequest, httpResponse)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    // ─── Web session cookie ─────────────────────────────────────────

    /** For the website, put the token in an HttpOnly cookie and leave it out of the JSON body. */
    private fun withSession(
        response: AuthResponse,
        httpRequest: jakarta.servlet.http.HttpServletRequest,
        httpResponse: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        if (!com.rajasthanexams.backend.config.WebSessionCookie.isWebRequest(httpRequest)) {
            return ResponseEntity.ok(response)
        }
        com.rajasthanexams.backend.config.WebSessionCookie.set(httpRequest, httpResponse, response.token)
        return ResponseEntity.ok(response.copy(token = ""))
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (website)", description = "Ends the website session and clears its login cookie.")
    fun logout(
        httpRequest: jakarta.servlet.http.HttpServletRequest,
        httpResponse: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<ApiResponse> {
        val token = com.rajasthanexams.backend.config.WebSessionCookie.read(httpRequest)
        if (token != null) {
            try {
                val username = jwtService.extractUsername(token)
                val key = com.rajasthanexams.backend.service.JwtService.sessionKey(username, jwtService.extractClient(token))
                if (redisService.getValue(key) == token) redisService.deleteValue(key)
            } catch (e: Exception) {
                // Expired or invalid token: nothing to end on the server.
            }
        }
        com.rajasthanexams.backend.config.WebSessionCookie.clear(httpRequest, httpResponse)
        return ResponseEntity.ok(ApiResponse("Logged out", true))
    }

    // ─── Profile Update ─────────────────────────────────────────────

    @PostMapping("/update-profile")
    @Operation(
        summary = "Update Profile",
        description = "Updates the logged-in user's name, profile picture, optional mobile, and email (email only if none is set yet). Requires login."
    )
    fun updateProfile(
        @RequestBody request: UpdateProfileRequest,
        authentication: org.springframework.security.core.Authentication?
    ): ResponseEntity<ApiResponse> {
        // /api/auth/** is public, so check login here. The user always comes from the token,
        // never from the request body (otherwise anyone could edit anyone's account).
        if (authentication == null || !authentication.isAuthenticated ||
            authentication is org.springframework.security.authentication.AnonymousAuthenticationToken
        ) {
            return ResponseEntity.status(401).body(ApiResponse("Please login again", false))
        }
        return try {
            authService.updateProfile(authentication.name, request)
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
