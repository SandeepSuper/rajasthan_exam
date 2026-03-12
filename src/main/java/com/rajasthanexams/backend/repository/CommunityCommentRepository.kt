package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.CommunityComment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommunityCommentRepository : JpaRepository<CommunityComment, UUID> {
    fun findAllByPostIdOrderByCreatedAtAsc(postId: UUID): List<CommunityComment>
}
