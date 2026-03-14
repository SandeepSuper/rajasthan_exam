package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val mobile: String,

    @Column(unique = true)
    var email: String? = null,

    @Column(name = "password_hash")
    val passwordHash: String? = null,

    @Column
    var name: String? = null,

    @Column(name = "profile_picture")
    var profilePicture: String? = null,

    @Enumerated(EnumType.STRING)
    val role: Role = Role.STUDENT,

    @Column(name = "is_premium")
    val isPremium: Boolean = false,

    @Column(name = "coins")
    var coins: Int? = 0,

    /** Unique referral code for this user, e.g. "RAJ-9X4K2A" */
    @Column(name = "refer_code", unique = true)
    var referCode: String? = null,

    /** How many new users signed up using this user's referral code */
    @Column(name = "referred_count")
    var referredCount: Int? = 0,

    /** The exact amount of coins given to the referrer when this user joined via their code */
    @Column(name = "referrer_reward_amount")
    var referrerRewardAmount: Int? = null,

    /** Total historical coins earned from referrals (prevents retroactive reduction/inflation mapping) */
    @Column(name = "historical_referral_coins_earned")
    var historicalReferralCoinsEarned: Int? = 0,

    /** The referral code this user used when registering (if any) */
    @Column(name = "referred_by")
    var referredBy: String? = null,

    /** If true, user cannot post in community / Ask Doubt section */
    @Column(name = "is_community_blocked", nullable = false)
    var isCommunityBlocked: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class Role {
    ADMIN, STUDENT
}
