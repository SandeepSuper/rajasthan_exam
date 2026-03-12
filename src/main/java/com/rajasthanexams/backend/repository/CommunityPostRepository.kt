package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.CommunityPost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CommunityPostRepository : JpaRepository<CommunityPost, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<CommunityPost>
    fun findByExamIdOrderByCreatedAtDesc(examId: UUID): List<CommunityPost>
}
