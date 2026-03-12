package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "community_likes", uniqueConstraints = [
    UniqueConstraint(columnNames = ["postId", "userId"])
])
data class CommunityLike(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val postId: UUID,

    @Column(nullable = false)
    val userId: UUID
)
