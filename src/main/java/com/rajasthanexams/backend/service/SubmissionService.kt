package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.dto.SubmitTestRequest
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

        // Calculate Coins
        val participationCoins = 10
        val performanceCoins = if (score > 0) score.toInt() else 0
        val bonusCoins = if (accuracy > 80.0) 20 else 0
        val totalCoinsEarned = participationCoins + performanceCoins + bonusCoins

        // Update User Coins
        // Update User Coins
        val currentCoins = user.coins ?: 0
        user.coins = currentCoins + totalCoinsEarned
        userRepository.save(user)
        println("Updated user ${user.id} coins to ${user.coins}")

        // Save Attempt
        val attempt = TestAttempt(
            user = user,
            test = test,
            score = score,
            totalQuestions = totalQuestions,
            timeTakenSeconds = request.timeTaken.coerceAtLeast(0),
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

        // Prepare Solutions (Simple map of Q_ID -> Correct_Index)
        // In real app, we return full solution text
        val solutions = questions.associate { it.id!! to it.correctOptionIndex }

        return TestResultResponse(
            attemptId = savedAttempt.id!!,
            score = score,
            totalQuestions = totalQuestions,
            accuracy = accuracy,
            correctAnswers = correctCount,
            coinsEarned = totalCoinsEarned,
            newTotalCoins = user.coins ?: 0, // Added
            solutions = solutions
        )
    }
}
