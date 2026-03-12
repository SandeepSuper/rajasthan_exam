package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "tests")
data class Test(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    val exam: Exam,

    @Column(nullable = false)
    val title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TestType, // MOCK, TOPIC, PYQ, FULL

    @Column(name = "duration_minutes")
    val durationMinutes: Int = 60,

    @Column(name = "total_questions")
    val totalQuestions: Int = 0,

    @Column(name = "is_premium")
    val isPremium: Boolean = false,

    @Column(name = "is_live")
    val isLive: Boolean? = false,

    @Column(name = "starts_at")
    val startsAt: java.time.LocalDateTime? = null,

    @Column(name = "ends_at")
    val endsAt: java.time.LocalDateTime? = null,

    @Column(name = "allow_previous")
    val allowPrevious: Boolean? = true,

    @Column(name = "allow_solution")
    val allowSolution: Boolean? = true,

    @Column(name = "section_lock")
    val sectionLock: Boolean? = false,

    @Column(name = "show_result_immediately")
    val showResultImmediately: Boolean? = true,

    @Column(name = "marks_per_question_override")
    val marksPerQuestionOverride: Double? = null,

    @Column(name = "total_marks")
    val totalMarks: Double? = null
) {
    val negativeMarks: Double
        get() = exam.negativeMarks ?: 0.0

    val marksPerQuestion: Double
        get() = marksPerQuestionOverride ?: exam.marksPerQuestion ?: 1.0
}

enum class TestType {
    MOCK, TOPIC, PYQ, FULL, DAILY_QUIZ
}
