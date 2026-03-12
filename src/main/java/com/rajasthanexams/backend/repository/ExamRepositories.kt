package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.Exam
import com.rajasthanexams.backend.model.Question
import com.rajasthanexams.backend.model.Test
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExamRepository : JpaRepository<Exam, UUID> {
    fun findByTitle(title: String): Exam?
}

@Repository
interface TestRepository : JpaRepository<Test, UUID> {
    fun findByExamId(examId: UUID): List<Test>
    fun countByExamId(examId: UUID): Int
    
    // Fetch active or upcoming live tests
    fun findByIsLiveTrueAndEndsAtAfter(currentTime: java.time.LocalDateTime): List<Test>
}

@Repository
interface QuestionRepository : JpaRepository<Question, UUID> {
    fun findByTestId(testId: UUID): List<Question>
}
