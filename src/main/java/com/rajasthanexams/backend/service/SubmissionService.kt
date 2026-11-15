package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.dto.SubmitTestRequest
import com.rajasthanexams.backend.dto.SolutionDetail
import com.rajasthanexams.backend.dto.TestResultResponse
import com.rajasthanexams.backend.model.TestAttempt
import com.rajasthanexams.backend.repository.QuestionRepository
import com.rajasthanexams.backend.repository.TestAttemptRepository
import com.rajasthanexams.backend.repository.TestRepository
import com.rajasthanexams.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SubmissionService(
    private val testRepository: TestRepository,
    private val questionRepository: QuestionRepository,
    private val testAttemptRepository: TestAttemptRepository,
    private val userRepository: UserRepository,
    private val redisService: RedisService
) {

    companion object {
        /** Extra time allowed past the limit (network delay, auto-submit) before a live attempt stops counting. */
        private const val OVERTIME_GRACE_SECONDS = 120
    }

    @Transactional
    fun submitTest(userId: UUID, request: SubmitTestRequest): TestResultResponse {
        val test = testRepository.findById(request.testId).orElseThrow {
            IllegalArgumentException("Test not found")
        }
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        val questions = questionRepository.findByTestId(request.testId)
        val questionMap = questions.associateBy { it.id!! }
        val negativeMarks = test.negativeMarks ?: 0.0

        var correctCount = 0
        var totalAnswered = 0 // Using map size effectively
        var score = 0.0

        // Calculate Score
        request.answers.forEach { (questionId, selectedOption) ->
            val question = questionMap[questionId]
            if (question != null) {
                totalAnswered++
                if (question.correctOptionIndex == selectedOption) {
                    correctCount++
                    score += test.marksPerQuestion
                } else {
                    score -= negativeMarks
                }
            }
        }

        val totalQuestions = questions.size
        val accuracy = if (totalAnswered > 0) (correctCount.toFloat() / totalAnswered.toFloat()) * 100 else 0f

        // Full answers + explanations go back to the user only after they submit.
        val solutions = questions.associate { it.id!! to it.correctOptionIndex }
        val solutionDetails = questions.associate {
            it.id!! to SolutionDetail(it.correctOptionIndex, it.solutionHi, it.solutionEn)
        }

        // Time taken comes from the server clock (started when the questions were loaded), not from
        // the browser, so it can't be faked. Old clients / a Redis restart fall back to the sent value.
        val limitSeconds = test.durationMinutes * 60
        val startMs = try { redisService.getAttemptStart(userId.toString(), test.id.toString()) } catch (e: Exception) { null }
        val serverElapsed = startMs?.let { ((System.currentTimeMillis() - it) / 1000).toInt() }
        val timeTaken = (serverElapsed ?: request.timeTaken).coerceIn(0, limitSeconds)
        val overTime = serverElapsed != null && serverElapsed > limitSeconds + OVERTIME_GRACE_SECONDS
        try { redisService.clearAttemptStart(userId.toString(), test.id.toString()) } catch (e: Exception) { }

        // Live tests: only the first submission made during the live window counts.
        // Anything else (a retake, or a first try after the test ended, when answers are public)
        // is scored for the user's own practice but saves nothing and earns no coins.
        if (test.isLive == true) {
            val alreadyAttempted = testAttemptRepository.existsByTestIdAndUserId(test.id!!, userId)
            val inWindow = LiveTestRules.isWithinRankedWindow(test)
            if (alreadyAttempted || !inWindow || overTime) {
                return TestResultResponse(
                    attemptId = null,
                    score = score,
                    totalQuestions = totalQuestions,
                    accuracy = accuracy,
                    correctAnswers = correctCount,
                    coinsEarned = 0,
                    newTotalCoins = user.coins ?: 0,
                    solutions = solutions,
                    solutionDetails = solutionDetails,
                    countedForRank = false,
                    message = when {
                        alreadyAttempted -> "Aap yeh live test pehle de chuke hain. Yeh attempt sirf practice hai, rank aur coins par asar nahi hoga."
                        !inWindow -> "Live test ka samay khatam ho chuka hai. Yeh attempt sirf practice hai, rank aur coins par asar nahi hoga."
                        else -> "Test ka time limit se zyada samay laga, isliye yeh attempt rank mein nahi gina gaya."
                    }
                )
            }
        }

        // Coins are earned once per test. Retakes are still saved (for performance stats) but earn
        // nothing, otherwise a user could repeat the same test, with the answers already known,
        // to farm unlimited coins.
        val isFirstAttempt = !testAttemptRepository.existsByTestIdAndUserId(test.id!!, userId)
        val totalCoinsEarned = if (isFirstAttempt) {
            val participationCoins = 10
            val performanceCoins = if (score > 0) score.toInt() else 0
            val bonusCoins = if (accuracy > 80.0) 20 else 0
            participationCoins + performanceCoins + bonusCoins
        } else 0

        if (totalCoinsEarned > 0) {
            user.coins = (user.coins ?: 0) + totalCoinsEarned
            userRepository.save(user)
        }

        // Save Attempt
        val attempt = TestAttempt(
            user = user,
            test = test,
            score = score,
            totalQuestions = totalQuestions,
            timeTakenSeconds = timeTaken,
            accuracy = accuracy,
            coinsEarned = totalCoinsEarned
        )
        val savedAttempt = testAttemptRepository.save(attempt)

        // Update Leaderboard
        try {
            redisService.updateLeaderboard(test.exam.id.toString(), userId.toString(), score.toDouble())
        } catch (e: Exception) {
            println("Redis Error: ${e.message}")
        }

        return TestResultResponse(
            attemptId = savedAttempt.id!!,
            score = score,
            totalQuestions = totalQuestions,
            accuracy = accuracy,
            correctAnswers = correctCount,
            coinsEarned = totalCoinsEarned,
            newTotalCoins = user.coins ?: 0, // Added
            solutions = solutions,
            solutionDetails = solutionDetails,
            message = if (isFirstAttempt) null else "Is test ke coins pehli baar dene par mil chuke hain. Dobara dene par coins nahi milte."
        )
    }
}
