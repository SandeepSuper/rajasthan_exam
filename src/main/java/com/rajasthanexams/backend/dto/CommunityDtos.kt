package com.rajasthanexams.backend.dto

import java.util.UUID

data class CreatePostRequest(
    val userId: UUID,
    val userName: String,
    val userProfilePicture: String?,
    val content: String,
    val subject: String,
    val category: String,
    val examId: UUID? = null   // optional — null means "All Exams"
)

data class CreateCommentRequest(
    val userId: UUID,
    val userName: String,
    val userProfilePicture: String?,
    val content: String
)
data class CommunityPostResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val userProfilePicture: String?,
    val content: String,
    val subject: String,
    val category: String,
    val examId: UUID?,
    val upvotes: Int,
    val commentCount: Int,
    val viewCount: Int,
    val isLiked: Boolean,
    val verifiedAnswer: String?,
    val createdAt: String
)

