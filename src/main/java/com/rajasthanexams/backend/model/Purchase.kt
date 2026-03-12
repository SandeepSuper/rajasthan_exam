package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "purchases")
data class Purchase(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "exam_id", nullable = false)
    val examId: String,

    @Column(name = "purchased_at")
    val purchasedAt: LocalDateTime = LocalDateTime.now()
)
