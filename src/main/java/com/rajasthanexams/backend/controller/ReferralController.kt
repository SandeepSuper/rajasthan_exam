package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.model.AppConfig
import com.rajasthanexams.backend.repository.AppConfigRepository
import com.rajasthanexams.backend.repository.UserRepository
import com.rajasthanexams.backend.service.JwtService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class ReferralInfoResponse(
    val referCode: String,
    val referredCount: Int,
    val coinsEarned: Int  // referredCount * config.referrerCoinReward
)

data class TopReferrerResponse(
    val name: String,
    val referredCount: Int,
    val avatarId: String?
)

data class ReferredUserResponse(
    val name: String,
    val joinedAt: String,
    val avatarId: String?,
    val coinsEarned: Int
)

@RestController
@RequestMapping("/api/users")
@Tag(name = "Referral", description = "Referral code and leaderboard")
@SecurityRequirement(name = "bearerAuth")
class ReferralController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val appConfigRepository: AppConfigRepository
) {

    @GetMapping("/referral")
    @Operation(summary = "Get My Referral Info", description = "Returns the current user's referral code and stats.")
    fun getMyReferralInfo(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ReferralInfoResponse> {
        val user = getUserFromToken(authHeader)
        val code = user.referCode ?: "RAJ-XXXXXX"
        
        val config = appConfigRepository.findById(1L).orElseGet {
            appConfigRepository.save(AppConfig())
        }
        
        return ResponseEntity.ok(
            ReferralInfoResponse(
                referCode = code,
                referredCount = user.referredCount ?: 0,
                coinsEarned = user.historicalReferralCoinsEarned ?: 0
            )
        )
    }

    @GetMapping("/top-referrers")
    @Operation(summary = "Top Referrers", description = "Returns the top 5 users by referral count.")
    fun getTopReferrers(): ResponseEntity<List<TopReferrerResponse>> {
        val top = userRepository.findTopReferrers(PageRequest.of(0, 5))
        val result = top.map { u ->
            TopReferrerResponse(
                name = u.name ?: "Student",
                referredCount = u.referredCount ?: 0,
                avatarId = u.profilePicture
            )
        }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/my-referrals")
    @Operation(summary = "My Referrals", description = "Returns the list of users who joined using the current user's referral code.")
    fun getMyReferrals(@RequestHeader("Authorization") authHeader: String): ResponseEntity<List<ReferredUserResponse>> {
        val user = getUserFromToken(authHeader)
        val code = user.referCode ?: return ResponseEntity.ok(emptyList())

        val config = appConfigRepository.findById(1L).orElseGet {
            appConfigRepository.save(AppConfig())
        }

        val referredUsers = userRepository.findByReferredBy(code)
        val result = referredUsers.map { u ->
            val dateStr = u.createdAt.toLocalDate().toString()
            ReferredUserResponse(
                name = u.name ?: "Student",
                joinedAt = dateStr,
                avatarId = u.profilePicture,
                coinsEarned = u.referrerRewardAmount ?: 50 // Older referrals default to 50
            )
        }
        return ResponseEntity.ok(result)
    }

    private fun getUserFromToken(authHeader: String): com.rajasthanexams.backend.model.User {
        val token = authHeader.substring(7)
        val username = jwtService.extractUsername(token)
        return userRepository.findByMobile(username)
            .orElseThrow { IllegalArgumentException("User not found") }
    }
}
