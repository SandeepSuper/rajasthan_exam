package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "test_attempts")
data class TestAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    val test: Test,

    @Column(nullable = false)
    val score: Double = 0.0,

    @Column(name = "total_questions")
    val totalQuestions: Int = 0,

    @Column(name = "time_taken")
    val timeTakenSeconds: Int = 0,

    @Column
    val accuracy: Float = 0.0f,

    @Column(name = "coins_earned")
    val coinsEarned: Int? = 0,

    @Column(name = "attempt_date")
    val attemptDate: LocalDateTime = LocalDateTime.now()
)
