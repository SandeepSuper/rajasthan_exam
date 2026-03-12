package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.Bookmark
import com.rajasthanexams.backend.model.EntityType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BookmarkRepository : JpaRepository<Bookmark, UUID> {
    fun findByUserId(userId: UUID): List<Bookmark>
    fun deleteByUserIdAndEntityTypeAndEntityId(userId: UUID, entityType: EntityType, entityId: UUID)
}
