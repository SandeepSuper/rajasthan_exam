package com.rajasthanexams.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.rajasthanexams.backend.repository.AppConfigRepository
import com.rajasthanexams.backend.model.AppConfig

data class AppConfigResponse(
    val playStoreUrl: String,
    val referrerCoinReward: Int = 50,
    val refereeCoinReward: Int = 20,
    val shareMessage: String
)

@RestController
@RequestMapping("/api/config")
@Tag(name = "App Config", description = "Remote app configuration values")
class AppConfigController(
    private val appConfigRepository: AppConfigRepository
) {

    @GetMapping
    @Operation(summary = "Get App Config", description = "Returns remote config values like Play Store URL and referral coin rewards.")
    fun getConfig(): ResponseEntity<AppConfigResponse> {
        val config = appConfigRepository.findById(1L).orElseGet {
            appConfigRepository.save(AppConfig())
        }
        
        return ResponseEntity.ok(
            AppConfigResponse(
                playStoreUrl = config.playStoreUrl,
                referrerCoinReward = config.referrerCoinReward,
                refereeCoinReward = config.refereeCoinReward,
                shareMessage = config.shareMessageTemplate.replace("{CODE}", "").replace("{URL}", config.playStoreUrl)
            )
        )
    }
}
