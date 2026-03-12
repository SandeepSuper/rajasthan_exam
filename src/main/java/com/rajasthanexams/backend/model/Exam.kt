package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "exams")
data class Exam(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val title: String,

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @Column(nullable = false)
    val category: String, // CET, REET, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "language_supported")
    val languageSupported: Language = Language.BOTH,

    @Column(name = "negative_marks")
    val negativeMarks: Double? = 0.0,

    @Column(name = "marks_per_question")
    val marksPerQuestion: Double? = 1.0,

    @Column(name = "is_premium")
    val isPremium: Boolean? = false,

    @Column
    val price: Double? = 0.0,

    @Column
    val currency: String? = "INR",

    @Column(name = "discount_percent")
    val discountPercent: Int? = 0
)

enum class Language {
    HI, EN, BOTH
}
