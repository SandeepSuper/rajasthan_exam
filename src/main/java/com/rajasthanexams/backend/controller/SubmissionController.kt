package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.SubmitTestRequest
import com.rajasthanexams.backend.dto.TestResultResponse
import com.rajasthanexams.backend.service.JwtService
import com.rajasthanexams.backend.service.SubmissionService
import com.rajasthanexams.backend.repository.UserRepository
import com.rajasthanexams.backend.repository.TestAttemptRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/tests")
@Tag(name = "Test Submission", description = "APIs for submitting tests and getting results")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
class SubmissionController(
    private val submissionService: SubmissionService,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val testAttemptRepository: TestAttemptRepository
) {

    @PostMapping("/submit")
    @Operation(summary = "Submit Test Answer", description = "Submits answers, calculates score, and returns full solution.")
    fun submitTest(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: SubmitTestRequest
    ): ResponseEntity<TestResultResponse> {
        val token = authHeader.substring(7)
        val subject = jwtService.extractUsername(token)
        val user = userRepository.findByEmail(subject)
            .orElseGet { userRepository.findByMobile(subject).orElse(null) }
            ?: throw IllegalArgumentException("User not found")
        val result = submissionService.submitTest(user.id!!, request)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/performance")
    @Operation(summary = "User Performance Stats", description = "Returns real performance analytics for the authenticated user.")
    fun getPerformance(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<Map<String, Any?>> {
        return try {
            val token = authHeader.substring(7)
            val subject = jwtService.extractUsername(token)
            val user = userRepository.findByEmail(subject)
                .orElseGet { userRepository.findByMobile(subject).orElse(null) }
                ?: throw IllegalArgumentException("User not found")

            val attempts = testAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.id!!)
            val totalTests = attempts.size
            val avgAccuracy = if (totalTests > 0) attempts.map { it.accuracy.toDouble() }.average() else 0.0
            val bestScore  = attempts.maxOfOrNull { it.score } ?: 0.0
            val totalTimeSecs = attempts.sumOf { it.timeTakenSeconds.coerceAtLeast(0) }

            // Last 7 attempts oldest → newest for chart
            val recent7 = attempts.take(7).reversed()
            val weeklyScores     = recent7.map { it.score }
            val weeklyAccuracies = recent7.map { it.accuracy.toDouble() }
            val weeklyDates      = recent7.map { it.attemptDate.toLocalDate().toString() }

            // Weak topics: group by test title, sort by avg accuracy asc → worst first
            val weakTopics = attempts
                .groupBy { it.test.title }
                .map { (title, list) -> title to list.map { it.accuracy }.average() }
                .sortedBy { it.second }
                .take(3)
                .map { it.first }

            ResponseEntity.ok(mapOf(
                "totalTests"       to totalTests,
                "avgAccuracy"      to String.format("%.1f", avgAccuracy).toDouble(),
                "bestScore"        to bestScore,
                "totalTimeSecs"    to totalTimeSecs,
                "weeklyScores"     to weeklyScores,
                "weeklyAccuracies" to weeklyAccuracies,
                "weeklyDates"      to weeklyDates,
                "weakTopics"       to weakTopics
            ))
        } catch (e: Exception) {
            println("getPerformance error: ${e.message}")
            ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }
}

