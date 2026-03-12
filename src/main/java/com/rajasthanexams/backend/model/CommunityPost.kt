package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "community_posts")
data class CommunityPost(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "exam_id", nullable = true)
    val examId: UUID? = null,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val userName: String,

    @Column(nullable = true)
    val userProfilePicture: String? = null,

    @Column(nullable = false, length = 1000)
    val content: String,

    @Column(nullable = false)
    val subject: String, // e.g., History, Geography

    @Column(nullable = false)
    val category: String, // e.g., "Culture", "Physical"

    @Column(nullable = false)
    var upvotes: Int = 0,

    @Column(nullable = false)
    var commentCount: Int = 0,

    @Column(nullable = true)
    var viewCount: Int? = 0,

    @Column(length = 1000)
    val verifiedAnswer: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
