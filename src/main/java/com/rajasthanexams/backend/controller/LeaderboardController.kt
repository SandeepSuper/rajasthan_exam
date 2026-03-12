package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.service.RedisService
import com.rajasthanexams.backend.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Leaderboard", description = "Real-time ranking APIs")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/leaderboard")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
class LeaderboardController(
    private val leaderboardService: com.rajasthanexams.backend.service.LeaderboardService
) {

    @GetMapping
    @Operation(summary = "Get Leaderboard", description = "Get top rankers for a specific test or global leaderboard.")
    fun getLeaderboard(
        @RequestParam(required = false) testId: String?,
        authentication: org.springframework.security.core.Authentication
    ): ResponseEntity<List<LeaderboardEntry>> {
        val currentUserId = if (authentication.principal is org.springframework.security.core.userdetails.UserDetails) {
            (authentication.principal as org.springframework.security.core.userdetails.UserDetails).username
        } else {
            authentication.principal.toString()
        }
        val leaderboard = leaderboardService.getLeaderboard(testId, currentUserId)
        return ResponseEntity.ok(leaderboard)
    }

    @org.springframework.web.bind.annotation.PostMapping("/sync-coins")
    @Operation(summary = "Sync Coins", description = "Sync local coin balance to server (one-time fix for lost data).")
    fun syncCoins(
        @org.springframework.web.bind.annotation.RequestBody request: SyncCoinsRequest,
        authentication: org.springframework.security.core.Authentication
    ): ResponseEntity<Map<String, Any>> {
        val mobile = if (authentication.principal is org.springframework.security.core.userdetails.UserDetails) {
            (authentication.principal as org.springframework.security.core.userdetails.UserDetails).username
        } else {
            authentication.principal.toString()
        }
        
        return try {
            val result = leaderboardService.syncCoins(mobile, request.coins)
            ResponseEntity.ok(mapOf("message" to "Coins synced", "coins" to result))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Sync failed"), "coins" to 0))
        }
    }
}

data class SyncCoinsRequest(val coins: Int)

data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val rank: Int,
    val score: Double,
    val totalMarks: Double = 0.0,
    val timeTaken: Int,
    val coins: Int = 0,
    val exam: String? = null,
    val avatarUrl: String? = null
)
