package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "community_comments")
data class CommunityComment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val postId: UUID,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val userName: String,

    @Column(nullable = true)
    val userProfilePicture: String? = null,

    @Column(nullable = false, length = 1000)
    val content: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
