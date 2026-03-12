package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "question_reports")
data class QuestionReport(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "test_id", nullable = false)
    val testId: String,

    @Column(name = "question_id", nullable = false)
    val questionId: String,

    @Column(name = "bug_type", nullable = false)
    val bugType: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
