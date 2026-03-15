package com.rajasthanexams.backend.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class RedisService(
    private val redisTemplate: StringRedisTemplate
) {
    // Save OTP with TTL
    fun saveOtp(mobile: String, otp: String, ttlMinutes: Long = 5) {
        val key = "auth:otp:$mobile"
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(ttlMinutes))
    }

    // Get OTP
    fun getOtp(mobile: String): String? {
        val key = "auth:otp:$mobile"
        return redisTemplate.opsForValue().get(key)
    }

    // Delete OTP (after successful verification)
    fun deleteOtp(mobile: String) {
        val key = "auth:otp:$mobile"
        redisTemplate.delete(key)
    }

    // Leaderboard
    fun updateLeaderboard(examId: String, userId: String, score: Double) {
        val key = "leaderboard:weekly:$examId"
        // ZADD: Add or update score
        redisTemplate.opsForZSet().add(key, userId, score)
        // Set TTL for weekly leaderboard (e.g., 7 days) if not exists
        redisTemplate.expire(key, Duration.ofDays(7))
    }

    fun getTopRankers(examId: String, limit: Long = 10): Set<String> {
        val key = "leaderboard:weekly:$examId"
        // ZREVRANGE: Get top scores (high to low)
        return redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1) ?: emptySet()
    }

    // ─── Rate Limiting for Community Posts (Ask Doubt) ────────────────────────
    // Two-level limit:
    //   1. Per-day: max 30 posts per day (fair usage)
    //   2. Per-minute burst: max 5 posts per 60 seconds (spam protection)
    fun checkPostRateLimit(userId: String) {
        // ── Daily limit check ──────────────────────────────────────────────────
        val dailyKey = "rate:community:post:daily:$userId"
        val dailyCount = redisTemplate.opsForValue().increment(dailyKey) ?: 1L

        if (dailyCount == 1L) {
            // First post of the day — expire key after 24 hours
            redisTemplate.expire(dailyKey, Duration.ofDays(1))
        }

        if (dailyCount > 30) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Aap 1 din mein sirf 30 doubt post kar sakte hain. Kal dobara try karein."
            )
        }

        // ── Per-minute burst check ─────────────────────────────────────────────
        val minuteKey = "rate:community:post:minute:$userId"
        val minuteCount = redisTemplate.opsForValue().increment(minuteKey) ?: 1L

        if (minuteCount == 1L) {
            // First request in this minute window
            redisTemplate.expire(minuteKey, Duration.ofSeconds(60))
        }

        if (minuteCount > 5) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Aap 1 minute mein sirf 5 doubt post kar sakte hain. Thodi der baad try karein."
            )
        }
    }

    // ─── Rate Limiting for Community Comments ────────────────────────
    // Two-level limit identical to posts:
    //   1. Per-day: max 30 comments per day
    //   2. Per-minute burst: max 5 comments per 60 seconds
    fun checkCommentRateLimit(userId: String) {
        // ── Daily limit check ──────────────────────────────────────────────────
        val dailyKey = "rate:community:comment:daily:$userId"
        val dailyCount = redisTemplate.opsForValue().increment(dailyKey) ?: 1L

        if (dailyCount == 1L) {
            // First comment of the day — expire key after 24 hours
            redisTemplate.expire(dailyKey, Duration.ofDays(1))
        }

        if (dailyCount > 30) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Aap 1 din mein sirf 30 comments kar sakte hain. Kal dobara try karein."
            )
        }

        // ── Per-minute burst check ─────────────────────────────────────────────
        val minuteKey = "rate:community:comment:minute:$userId"
        val minuteCount = redisTemplate.opsForValue().increment(minuteKey) ?: 1L

        if (minuteCount == 1L) {
            // First request in this minute window
            redisTemplate.expire(minuteKey, Duration.ofSeconds(60))
        }

        if (minuteCount > 5) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Aap 1 minute mein sirf 5 comments kar sakte hain. Thodi der baad try karein."
            )
        }
    }

    // ─── Admin Helpers ─────────────────────────────────────────────────────────

    /** Read current value of a Redis counter key (returns 0 if key doesn't exist) */
    fun getCounter(key: String): Long =
        redisTemplate.opsForValue().get(key)?.toLongOrNull() ?: 0L

    /** Get remaining TTL in seconds for a key (-1 = no expiry, -2 = key not found) */
    fun getTtlSeconds(key: String): Long =
        redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS)

    /** Delete a Redis key (used to reset rate limits) */
    fun deleteKey(key: String) {
        redisTemplate.delete(key)
    }
}
