package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.CreateReportDto
import com.rajasthanexams.backend.model.QuestionReport
import com.rajasthanexams.backend.repository.QuestionReportRepository
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
class ReportController(
    private val reportRepository: QuestionReportRepository
) {

    @Operation(summary = "Submit a bug report")
    @PostMapping("/api/reports")
    fun createReport(
        @RequestBody request: CreateReportDto,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<Any> {
        if (userId == null) {
            return ResponseEntity.status(401).body(mapOf("error" to "User not authenticated"))
        }
        // userId is actually the username/mobile from the token


        val report = QuestionReport(
            userId = userId,
            testId = request.testId,
            questionId = request.questionId,
            bugType = request.bugType,
            description = request.description,
            createdAt = LocalDateTime.now()
        )

        reportRepository.save(report)

        return ResponseEntity.ok(mapOf("message" to "Report submitted successfully"))
    }

    @Operation(summary = "Get all bug reports (Admin)")
    @GetMapping("/api/admin/reports") // Updated endpoint as requested
    fun getBugReports(): ResponseEntity<List<QuestionReport>> {
        val reports = reportRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        return ResponseEntity.ok(reports)
    }
}
