package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.AuthResponse
import com.rajasthanexams.backend.dto.OtpRequest
import com.rajasthanexams.backend.dto.VerifyOtpRequest
import com.rajasthanexams.backend.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "OTP based login and verification")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/send-otp")
    @Operation(summary = "Send OTP", description = "Generates and sends an OTP to the provided mobile number.")
    fun sendOtp(@RequestBody request: OtpRequest): ResponseEntity<com.rajasthanexams.backend.dto.OtpResponse> {
        val result = authService.sendOtp(request.mobile)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies the OTP and returns a JWT token.")
    fun verifyOtp(@RequestBody request: VerifyOtpRequest): ResponseEntity<AuthResponse> {
        return try {
            val response = authService.verifyOtp(request.mobile, request.otp)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/update-profile")
    @Operation(summary = "Update Profile", description = "Updates user name, email, and profile picture.")
    fun updateProfile(@RequestBody request: com.rajasthanexams.backend.dto.UpdateProfileRequest): ResponseEntity<com.rajasthanexams.backend.dto.ApiResponse> {
        return try {
            authService.updateProfile(request.userId, request.name, request.email, request.profilePicture, request.referredByCode)
            ResponseEntity.ok(com.rajasthanexams.backend.dto.ApiResponse("Profile updated successfully", true))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(com.rajasthanexams.backend.dto.ApiResponse(e.message ?: "Update failed", false))
        }
    }
}
