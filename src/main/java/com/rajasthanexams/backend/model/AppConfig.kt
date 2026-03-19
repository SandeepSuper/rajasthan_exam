package com.rajasthanexams.backend.model

import jakarta.persistence.*

@Entity
@Table(name = "app_config")
data class AppConfig(
    @Id
    val id: Long = 1, // Only 1 row expected
    
    @Column(nullable = false)
    val playStoreUrl: String = "https://play.google.com/store/apps/details?id=com.rajasthanexams",
    
    @Column(nullable = false)
    val referrerCoinReward: Int = 50,
    
    @Column(nullable = false)
    val refereeCoinReward: Int = 20,
    
    @Column(nullable = false, length = 1000)
    val shareMessageTemplate: String = "Join Rajasthan Exam Prep and ace your government exams! 🎓\nUse my referral code: {CODE} when signing up to get FREE coins!\nDownload now: {URL}"
)
