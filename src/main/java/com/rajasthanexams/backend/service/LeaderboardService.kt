package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.controller.LeaderboardEntry
import com.rajasthanexams.backend.repository.TestAttemptRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
class LeaderboardService(
    private val testAttemptRepository: TestAttemptRepository,
    private val userRepository: com.rajasthanexams.backend.repository.UserRepository
) {

    fun getLeaderboard(testId: String?, currentUserId: String): List<LeaderboardEntry> {
        val topList: List<LeaderboardEntry>
        var currentUserRankEntry: LeaderboardEntry? = null
        
        // 1. Resolve current user UUID from Mobile (currentUserId param is mobile)
        val currentUserOpt = userRepository.findByMobile(currentUserId)
        val currentUser = if (currentUserOpt.isPresent) currentUserOpt.get() else null
        val currentUserIdStr = currentUser?.id?.toString() ?: ""

        if (testId.isNullOrEmpty()) {
            // Global Leaderboard: rank by coins first; if all 0, rank by test performance
            val topUsers = userRepository.findTopRankers(PageRequest.of(0, 50))

            // Check if meaningful coin data exists
            val hasCoinData = topUsers.any { (it.coins ?: 0) > 0 }

            topList = if (hasCoinData) {
                topUsers.mapIndexed { index, user ->
                    LeaderboardEntry(
                        userId = user.id.toString(),
                        name = user.name ?: "Unknown",
                        rank = index + 1,
                        score = (user.coins ?: 0).toDouble(),
                        timeTaken = 0,
                        coins = user.coins ?: 0,
                        exam = null,
                        avatarUrl = user.profilePicture
                    )
                }
            } else {
                // Fall back to test-attempt-based global ranking
                val allAttempts = testAttemptRepository.findAll()
                val perUser = allAttempts.groupBy { it.user.id!! }
                perUser.entries
                    .map { (_, attempts) ->
                        val u = attempts.first().user
                        val avgAcc = attempts.map { it.accuracy.toDouble() }.average()
                        val totalCoins = attempts.sumOf { it.coinsEarned ?: 0 }
                        Triple(u, avgAcc, totalCoins)
                    }
                    .sortedByDescending { it.second } // Sort by avg accuracy
                    .take(50)
                    .mapIndexed { index, (user, avgAcc, totalCoins) ->
                        LeaderboardEntry(
                            userId = user.id.toString(),
                            name = user.name ?: "Unknown",
                            rank = index + 1,
                            score = String.format("%.1f", avgAcc).toDouble(),
                            timeTaken = 0,
                            coins = totalCoins,
                            exam = null,
                            avatarUrl = user.profilePicture
                        )
                    }
            }

            // Check if current user is in top list using UUID comparison
            val userInTop = if (currentUserIdStr.isNotEmpty()) topList.find { it.userId == currentUserIdStr } else null
            
            if (userInTop == null && currentUser != null) {
                val userCoins = currentUser.coins ?: 0
                // Calculate Rank: Count users with MORE coins + 1
                val betterUsersCount = userRepository.countUsersWithMoreCoins(userCoins)
                val actualRank = (betterUsersCount + 1).toInt()
                
                currentUserRankEntry = LeaderboardEntry(
                    userId = currentUser.id.toString(),
                    name = currentUser.name ?: "Unknown",
                    rank = actualRank,
                    score = 0.0,
                    timeTaken = 0,
                    coins = userCoins,
                    avatarUrl = currentUser.profilePicture
                )
            }

        } else {
            // Test Specific Leaderboard (Score Based)
            val attempts = testAttemptRepository.findByTestIdOrderByScoreDescTimeTakenSecondsAsc(UUID.fromString(testId))
            val distinctAttempts = attempts.distinctBy { it.user.id }
            
            topList = distinctAttempts.take(50).mapIndexed { index, attempt ->
                val tm = attempt.test.totalMarks ?: (attempt.totalQuestions * attempt.test.marksPerQuestion)
                LeaderboardEntry(
                    userId = attempt.user.id.toString(),
                    name = attempt.user.name ?: "Unknown",
                    rank = index + 1,
                    score = attempt.score,
                    totalMarks = tm,
                    timeTaken = attempt.timeTakenSeconds,
                    coins = attempt.coinsEarned ?: 0,
                    exam = attempt.test.title,
                    avatarUrl = attempt.user.profilePicture
                )
            }

             // Check if current user is in top list
            val userInTop = if (currentUserIdStr.isNotEmpty()) topList.find { it.userId == currentUserIdStr } else null
            
            if (userInTop == null && currentUser != null) {
                // Find user's attempt in the full distinct list (if exists outside top 50)
                val userAttemptIndex = distinctAttempts.indexOfFirst { it.user.id.toString() == currentUserIdStr }
                if (userAttemptIndex != -1) {
                    val attempt = distinctAttempts[userAttemptIndex]
                    val tm = attempt.test.totalMarks ?: (attempt.totalQuestions * attempt.test.marksPerQuestion)
                    currentUserRankEntry = LeaderboardEntry(
                        userId = attempt.user.id.toString(),
                        name = attempt.user.name ?: "Unknown",
                        rank = userAttemptIndex + 1,
                        score = attempt.score,
                        totalMarks = tm,
                        timeTaken = attempt.timeTakenSeconds,
                        coins = attempt.coinsEarned ?: 0,
                        exam = attempt.test.title,
                        avatarUrl = attempt.user.profilePicture
                    )
                }
            }
        }

        // Return top list + current user if not present (append at end, UI handles it)
        return if (currentUserRankEntry != null) {
            topList + currentUserRankEntry
        } else {
            topList
        }
    }

    fun syncCoins(mobile: String, localCoins: Int): Int {
        val user = userRepository.findByMobile(mobile)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val serverCoins = user.coins ?: 0
        
        // Only update if local coins are higher (prevents abuse)
        if (localCoins > serverCoins) {
            user.coins = localCoins
            userRepository.save(user)
            println("Synced user ${user.id} coins: $serverCoins -> $localCoins")
            return localCoins
        }
        
        return serverCoins
    }
}
