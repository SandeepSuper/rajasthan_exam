package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable
import java.util.UUID
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByMobile(mobile: String): Optional<User>
    fun findByReferCode(referCode: String): Optional<User>
    fun findByReferredBy(referredBy: String): List<User>

    // Global Leaderboard queries
    @Query("SELECT u FROM User u ORDER BY COALESCE(u.coins, 0) DESC, u.name ASC")
    fun findTopRankers(pageable: Pageable): List<User>
    @Query("SELECT COUNT(u) FROM User u WHERE COALESCE(u.coins, 0) > :coins")
    fun countUsersWithMoreCoins(coins: Int): Long

    // Top Referrers
    @Query("SELECT u FROM User u WHERE u.referredCount > 0 ORDER BY u.referredCount DESC")
    fun findTopReferrers(pageable: Pageable): List<User>

    // Community Block
    fun findByIsCommunityBlocked(blocked: Boolean): List<User>
}

