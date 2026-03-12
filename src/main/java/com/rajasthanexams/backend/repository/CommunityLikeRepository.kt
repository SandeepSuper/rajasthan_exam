package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.CommunityLike
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommunityLikeRepository : JpaRepository<CommunityLike, UUID> {
    fun existsByPostIdAndUserId(postId: UUID, userId: UUID): Boolean
    fun deleteByPostIdAndUserId(postId: UUID, userId: UUID)
}
