package com.rajasthanexams.backend.dto

data class CreateReportDto(
    val testId: String,
    val questionId: String,
    val bugType: String,
    val description: String?
)
