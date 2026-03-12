package com.rajasthanexams.backend.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "questions")
data class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    val test: Test,

    @Column(name = "subject")
    val subject: String? = null,

    @Column(name = "text_hi")
    val textHi: String? = null,

    @Column(name = "text_en")
    val textEn: String? = null,

    // JSONB in Postgres
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_hi", columnDefinition = "jsonb")
    val optionsHi: List<String>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_en", columnDefinition = "jsonb")
    val optionsEn: List<String>? = null,

    @Column(name = "correct_option_index")
    val correctOptionIndex: Int,

    @Column(name = "solution_hi")
    val solutionHi: String? = null,

    @Column(name = "solution_en")
    val solutionEn: String? = null,

    @Column(name = "marks_per_question")
    val marksPerQuestion: Double? = null,

    @Column(name = "negative_marks")
    val negativeMarks: Double? = null
)
