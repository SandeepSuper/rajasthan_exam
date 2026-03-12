package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.model.Notification
import com.rajasthanexams.backend.repository.NotificationRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import com.rajasthanexams.backend.websocket.NotificationWebSocketHandler
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "App alerts and announcements")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
class NotificationController(
    private val notificationRepository: NotificationRepository,
    private val websocketHandler: NotificationWebSocketHandler
) {

    @GetMapping
    @Operation(summary = "Get Notifications", description = "List all notifications ordered by newest first.")
    fun getNotifications(): ResponseEntity<List<Notification>> {
        val notifications = notificationRepository.findAllByOrderByCreatedAtDesc()
        return ResponseEntity.ok(notifications)
    }

    // Admin endpoints
    @PostMapping
    @Operation(summary = "Create Notification", description = "Publish a new notification.")
    fun createNotification(@RequestBody request: Notification): ResponseEntity<Notification> {
        val saved = notificationRepository.save(request)
        // Broadcast the real-time notification to all connected Android devices instantly
        websocketHandler.broadcastNotification(saved)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Notification", description = "Remove a notification by ID.")
    fun deleteNotification(@PathVariable id: UUID): ResponseEntity<Void> {
        return if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Mark All Read", description = "Mark all notifications as read for the current user.")
    fun markAllRead(): ResponseEntity<Void> {
        val unread = notificationRepository.findByIsReadFalse()
        if (unread.isNotEmpty()) {
            val updated = unread.map { it.copy(isRead = true) }
            notificationRepository.saveAll(updated)
        }
        return ResponseEntity.noContent().build()
    }
}
