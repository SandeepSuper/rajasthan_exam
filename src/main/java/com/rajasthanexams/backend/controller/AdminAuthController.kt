package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.service.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin-only login endpoint.
 * Uses a simple username/password from application.yml (no OTP needed).
 * Returns a long-lived JWT token for use by the admin UI pages.
 */
@RestController
@RequestMapping("/api/admin-auth")
class AdminAuthController(
    private val jwtService: JwtService
) {

    @Value("\${admin.username:admin}")
    private lateinit var adminUsername: String

    @Value("\${admin.password:admin@rajasthan123}")
    private lateinit var adminPassword: String

    @PostMapping("/login")
    fun adminLogin(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val username = body["username"]?.trim() ?: ""
        val password = body["password"]?.trim() ?: ""

        if (username != adminUsername || password != adminPassword) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid credentials"))
        }

        // Generate a JWT using the admin username as the subject
        val token = jwtService.generateToken(adminUsername)
        return ResponseEntity.ok(mapOf(
            "token" to token,
            "username" to adminUsername,
            "message" to "Login successful"
        ))
    }
}
