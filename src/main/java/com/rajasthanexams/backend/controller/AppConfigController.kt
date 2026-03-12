package com.rajasthanexams.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class AppConfigResponse(
    val playStoreUrl: String,
    val referrerCoinReward: Int = 50,
    val refereeCoinReward: Int = 20,
    val shareMessage: String
)

@RestController
@RequestMapping("/api/config")
@Tag(name = "App Config", description = "Remote app configuration values")
class AppConfigController {

    @Value("\${app.play-store-url}")
    private lateinit var playStoreUrl: String

    @GetMapping
    @Operation(summary = "Get App Config", description = "Returns remote config values like Play Store URL and referral coin rewards.")
    fun getConfig(): ResponseEntity<AppConfigResponse> {
        return ResponseEntity.ok(
            AppConfigResponse(
                playStoreUrl = playStoreUrl,
                referrerCoinReward = 50,
                refereeCoinReward = 20,
                shareMessage = "Join Rajasthan Exam Prep and ace your government exams! 🎓\nUse my referral code: {CODE} when signing up to get FREE coins!\nDownload now: $playStoreUrl"
            )
        )
    }
}
