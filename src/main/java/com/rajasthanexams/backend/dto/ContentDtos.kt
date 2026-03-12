package com.rajasthanexams.backend.dto

import com.rajasthanexams.backend.model.Language
import com.rajasthanexams.backend.model.TestType
import java.util.UUID

data class CreateExamRequest(
    val title: String,
    val category: String,
    val iconUrl: String?,
    val languageSupported: Language = Language.BOTH,
    val negativeMarks: Double = 0.0,
    val marksPerQuestion: Double = 2.0,
    val isPremium: Boolean = false,
    val price: Double = 0.0,
    val discountPercent: Int = 0
)

data class UpdateExamRequest(
    val title: String?,
    val category: String?,
    val iconUrl: String?,
    val isPremium: Boolean?,
    val price: Double?,
    val currency: String?,
    val discountPercent: Int?
)

data class ExamDto(
    val id: UUID,
    val title: String,
    val category: String,
    val iconUrl: String?,
    val testCount: Int,
    val languageSupported: Language,
    val marksPerQuestion: Double = 1.0,
    val isPremium: Boolean = false,
    val price: Double = 0.0,
    val discountPercent: Int = 0,
    val isPurchased: Boolean = false
)

data class CreateTestRequest(
    val examId: UUID,
    val title: String,
    val type: TestType,
    val durationMinutes: Int,
    val totalQuestions: Int,
    val isPremium: Boolean = false,
    val isLive: Boolean = false,
    val startsAt: java.time.LocalDateTime? = null,
    val endsAt: java.time.LocalDateTime? = null,
    val allowPrevious: Boolean = true,
    val sectionLock: Boolean = false,
    val marksPerQuestion: Double? = null,
    val totalMarks: Double? = null
)

data class CreateQuestionRequest(
    val testId: UUID,
    val textHi: String?,
    val textEn: String?,
    val optionsHi: List<String>?,
    val optionsEn: List<String>?,
    val correctOptionIndex: Int,
    val solutionHi: String?,
    val solutionEn: String?,
    val subject: String? = null
)

data class TestResponse(
    val id: UUID,
    val title: String,
    val type: TestType,
    val durationMinutes: Int,
    val totalQuestions: Int,
    val isPremium: Boolean,
    val isLive: Boolean,
    val startsAt: java.time.LocalDateTime?,
    val endsAt: java.time.LocalDateTime?,
    val allowPrevious: Boolean,
    val sectionLock: Boolean,
    val negativeMarks: Double = 0.0,
    val marksPerQuestion: Double = 2.0,
    val totalMarks: Double? = null,
    val isAttempted: Boolean = false
)

data class QuestionResponse(
    val id: UUID,
    val textHi: String?,
    val textEn: String?,
    val optionsHi: List<String>?,
    val optionsEn: List<String>?,
    val correctOptionIndex: Int,
    val solutionHi: String?,
    val solutionEn: String?,
    val subject: String? = null,
    val marksPerQuestion: Double? = null,
    val negativeMarks: Double? = null
)

data class SubmitTestRequest(
    val testId: UUID,
    // Map of QuestionID to SelectedOptionIndex
    val answers: Map<UUID, Int>,
    val timeTaken: Int = 0
)

data class TestResultResponse(
    val attemptId: UUID,
    val score: Double,
    val totalQuestions: Int,
    val accuracy: Float,
    val correctAnswers: Int,
    val coinsEarned: Int = 0,
    val newTotalCoins: Int = 0, // Added
    val solutions: Map<UUID, Int> // Map of QuestionID to CorrectOptionIndex (Simple solution for now)
)

data class BookmarkRequest(
    val entityType: com.rajasthanexams.backend.model.EntityType,
    val entityId: UUID
)
