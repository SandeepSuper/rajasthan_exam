package com.rajasthanexams.backend.controller

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
    val coinsEarned: Int  // referredCount * 50
)

data class TopReferrerResponse(
    val name: String,
    val referredCount: Int,
    val avatarId: String?
)

@RestController
@RequestMapping("/api/users")
@Tag(name = "Referral", description = "Referral code and leaderboard")
@SecurityRequirement(name = "bearerAuth")
class ReferralController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    @GetMapping("/referral")
    @Operation(summary = "Get My Referral Info", description = "Returns the current user's referral code and stats.")
    fun getMyReferralInfo(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ReferralInfoResponse> {
        val user = getUserFromToken(authHeader)
        val code = user.referCode ?: "RAJ-XXXXXX"
        return ResponseEntity.ok(
            ReferralInfoResponse(
                referCode = code,
                referredCount = user.referredCount ?: 0,
                coinsEarned = (user.referredCount ?: 0) * 50
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

    private fun getUserFromToken(authHeader: String): com.rajasthanexams.backend.model.User {
        val token = authHeader.substring(7)
        val username = jwtService.extractUsername(token)
        return userRepository.findByMobile(username)
            .orElseThrow { IllegalArgumentException("User not found") }
    }
}
