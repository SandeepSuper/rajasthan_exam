package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<Notification>
    fun findByIsReadFalse(): List<Notification>
}
