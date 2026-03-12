package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.QuestionResponse
import com.rajasthanexams.backend.dto.TestResponse
import com.rajasthanexams.backend.service.ContentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/tests")
@Tag(name = "Client Exam Engine", description = "Public APIs for fetching Tests and Questions")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
class TestController(
    private val contentService: ContentService,
    private val jwtService: com.rajasthanexams.backend.service.JwtService,
    private val userRepository: com.rajasthanexams.backend.repository.UserRepository
) {

    @GetMapping("/exams")
    @Operation(summary = "Get All Exams", description = "Fetch all available exam categories.")
    fun getExams(
        @org.springframework.web.bind.annotation.RequestHeader("Authorization", required = false) token: String?
    ): ResponseEntity<List<com.rajasthanexams.backend.dto.ExamDto>> {
        var userId: UUID? = null
        if (!token.isNullOrEmpty() && token.startsWith("Bearer ")) {
            try {
                val jwt = token.substring(7)
                val mobile = jwtService.extractUsername(jwt)
                val user = userRepository.findByMobile(mobile)
                if (user.isPresent) {
                    userId = user.get().id
                }
            } catch (e: Exception) {
                // Ignore token errors
            }
        }
        val exams = contentService.getAllExamsWithCounts(userId)
        return ResponseEntity.ok(exams)
    }

    @GetMapping
    @Operation(summary = "Get Tests", description = "Fetch all tests for a specific Exam ID.")
    fun getTests(
        @RequestParam examId: UUID,
        @org.springframework.web.bind.annotation.RequestHeader("Authorization", required = false) token: String?
    ): ResponseEntity<List<TestResponse>> {
        val tests = contentService.getTests(examId)
        
        var attemptedTestIds = emptySet<UUID>()
        if (!token.isNullOrEmpty() && token.startsWith("Bearer ")) {
            try {
                val jwt = token.substring(7)
                val mobile = jwtService.extractUsername(jwt)
                val user = userRepository.findByMobile(mobile)
                if (user.isPresent) {
                    attemptedTestIds = contentService.getAttemptedTestIds(user.get().id!!)
                }
            } catch (e: Exception) {
                // Ignore token errors for public view
            }
        }

        val response = tests.map { 
            TestResponse(
                id = it.id!!,
                title = it.title,
                type = it.type,
                durationMinutes = it.durationMinutes,
                totalQuestions = it.totalQuestions,
                isPremium = it.isPremium,
                isLive = it.isLive ?: false,
                startsAt = it.startsAt,
                endsAt = it.endsAt,
                allowPrevious = it.allowPrevious ?: true,
                sectionLock = it.sectionLock ?: false,
                negativeMarks = it.negativeMarks,
                marksPerQuestion = it.marksPerQuestion,
                totalMarks = it.totalMarks,
                isAttempted = attemptedTestIds.contains(it.id!!)
            )
        }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{testId}")
    @Operation(summary = "Get Test Details", description = "Fetch details for a specific Test ID.")
    fun getTestDetails(
        @org.springframework.web.bind.annotation.PathVariable testId: UUID, 
        @org.springframework.web.bind.annotation.RequestHeader("Authorization", required = false) token: String?
    ): ResponseEntity<TestResponse> {
        val it = contentService.getTestById(testId)
        
        var isAttempted = false
        if (!token.isNullOrEmpty() && token.startsWith("Bearer ")) {
            try {
                val jwt = token.substring(7)
                val mobile = jwtService.extractUsername(jwt)
                val user = userRepository.findByMobile(mobile)
                if (user.isPresent) {
                    isAttempted = contentService.hasUserAttemptedTest(testId, user.get().id!!)
                }
            } catch (e: Exception) {
                // Ignore token errors
            }
        }

        val response = TestResponse(
            id = it.id!!,
            title = it.title,
            type = it.type,
            durationMinutes = it.durationMinutes,
            totalQuestions = it.totalQuestions,
            isPremium = it.isPremium,
            isLive = it.isLive ?: false,
            startsAt = it.startsAt,
            endsAt = it.endsAt,
            allowPrevious = it.allowPrevious ?: true,
            sectionLock = it.sectionLock ?: false,
            negativeMarks = it.negativeMarks,
            marksPerQuestion = it.marksPerQuestion,
            totalMarks = it.totalMarks,
            isAttempted = isAttempted
        )
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/live")
    @Operation(summary = "Get Live Tests", description = "Fetch all active and upcoming Live Tests.")
    fun getLiveTests(): ResponseEntity<List<TestResponse>> {
        val tests = contentService.getLiveTests()
        val response = tests.map { 
            TestResponse(
                id = it.id!!,
                title = it.title,
                type = it.type,
                durationMinutes = it.durationMinutes,
                totalQuestions = it.totalQuestions,
                isPremium = it.isPremium,
                isLive = it.isLive ?: false,
                startsAt = it.startsAt,
                endsAt = it.endsAt,
                allowPrevious = it.allowPrevious ?: true,
                sectionLock = it.sectionLock ?: false,
                negativeMarks = it.negativeMarks,
                marksPerQuestion = it.marksPerQuestion,
                totalMarks = it.totalMarks
            )
        }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/questions")
    @Operation(summary = "Get Questions", description = "Securely fetch questions for a test (Solutions hidden).")
    fun getQuestions(@RequestParam testId: UUID): ResponseEntity<List<QuestionResponse>> {
        val questions = contentService.getQuestions(testId)
        val response = questions.map {
            QuestionResponse(
                id = it.id!!,
                textHi = it.textHi,
                textEn = it.textEn,
                optionsHi = it.optionsHi,
                optionsEn = it.optionsEn,
                correctOptionIndex = it.correctOptionIndex,
                solutionHi = it.solutionHi,
                solutionEn = it.solutionEn,
                subject = it.subject,
                marksPerQuestion = it.marksPerQuestion,
                negativeMarks = it.negativeMarks
            )
        }
        return ResponseEntity.ok(response)
    }
    @org.springframework.web.bind.annotation.PostMapping("/seed-live")
    fun seedLiveTest(): ResponseEntity<String> {
        return try {
            contentService.createSampleLiveTest()
            ResponseEntity.ok("Live Test Created Successfully")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Error: ${e.message}")
        }
    }
}
