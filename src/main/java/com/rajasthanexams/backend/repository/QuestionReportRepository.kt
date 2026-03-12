package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.QuestionReport
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface QuestionReportRepository : JpaRepository<QuestionReport, UUID>
