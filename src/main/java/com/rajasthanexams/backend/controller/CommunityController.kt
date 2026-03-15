package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.model.CommunityPost
import com.rajasthanexams.backend.repository.CommunityPostRepository
import com.rajasthanexams.backend.repository.PurchaseRepository
import com.rajasthanexams.backend.repository.UserRepository
import com.rajasthanexams.backend.service.RedisService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/community")
class CommunityController(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: com.rajasthanexams.backend.repository.CommunityCommentRepository,
    private val communityLikeRepository: com.rajasthanexams.backend.repository.CommunityLikeRepository,
    private val redisService: RedisService,
    private val userRepository: UserRepository,
    private val purchaseRepository: PurchaseRepository
) {

    @GetMapping("/posts")
    fun getAllPosts(
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) examId: UUID?  // null = fetch all posts
    ): List<com.rajasthanexams.backend.dto.CommunityPostResponse> {
        val posts = if (examId != null)
            communityPostRepository.findByExamIdOrderByCreatedAtDesc(examId)
        else
            communityPostRepository.findAllByOrderByCreatedAtDesc()

        return posts.map { post ->
            val isLiked = if (userId != null) {
                communityLikeRepository.existsByPostIdAndUserId(post.id!!, userId)
            } else {
                false
            }
            com.rajasthanexams.backend.dto.CommunityPostResponse(
                id = post.id!!,
                userId = post.userId,
                userName = post.userName,
                userProfilePicture = post.userProfilePicture,
                content = post.content,
                subject = post.subject,
                category = post.category,
                examId = post.examId,
                upvotes = post.upvotes,
                commentCount = post.commentCount,
                viewCount = post.viewCount ?: 0,
                isLiked = isLiked,
                verifiedAnswer = post.verifiedAnswer,
                createdAt = post.createdAt.toString()
            )
        }
    }

    @PostMapping("/posts")
    fun createPost(@RequestBody request: com.rajasthanexams.backend.dto.CreatePostRequest): CommunityPost {
        // Block check: is this user banned from community?
        val user = userRepository.findById(request.userId).orElse(null)
        if (user?.isCommunityBlocked == true) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Aapko community mein post karne se block kar diya gaya hai. Admin se sampark karein."
            )
        }

        // Purchase check: if posting to an exam-specific tab, user must have purchased that exam
        if (request.examId != null) {
            val hasPurchased = purchaseRepository.existsByUserIdAndExamId(
                request.userId.toString(),
                request.examId.toString()
            )
            if (!hasPurchased) {
                throw ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Sirf purchased students is exam ka doubt post kar sakte hain."
                )
            }
        }

        // Rate limit: max 5 doubts per minute per user
        redisService.checkPostRateLimit(request.userId.toString())

        val post = CommunityPost(
            examId = request.examId,
            userId = request.userId,
            userName = request.userName,
            userProfilePicture = request.userProfilePicture ?: "",
            content = request.content,
            subject = request.subject,
            category = request.category
        )
        return communityPostRepository.save(post)
    }

    @GetMapping("/posts/{postId}/comments")
    fun getComments(@PathVariable postId: UUID): List<com.rajasthanexams.backend.model.CommunityComment> {
        return communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId)
    }

    @PostMapping("/posts/{postId}/comments")
    fun addComment(
        @PathVariable postId: UUID,
        @RequestBody request: com.rajasthanexams.backend.dto.CreateCommentRequest
    ): com.rajasthanexams.backend.model.CommunityComment {
        val post = communityPostRepository.findById(postId).orElseThrow { RuntimeException("Post not found") }
        
        // Rate limit: max 30 comments per day, 5 per minute per user
        redisService.checkCommentRateLimit(request.userId.toString())
        
        val comment = com.rajasthanexams.backend.model.CommunityComment(
            postId = postId,
            userId = request.userId,
            userName = request.userName,
            userProfilePicture = request.userProfilePicture ?: "",
            content = request.content
        )
        
        val savedComment = communityCommentRepository.save(comment)
        
        // Update comment count
        post.commentCount += 1
        communityPostRepository.save(post)
        
        return savedComment
    }

    @PutMapping("/posts/{postId}/view")
    fun incrementView(@PathVariable postId: UUID) {
        val post = communityPostRepository.findById(postId).orElseThrow { RuntimeException("Post not found") }
        post.viewCount = (post.viewCount ?: 0) + 1
        communityPostRepository.save(post)
    }

    @PutMapping("/posts/{postId}/like")
    fun toggleLike(@PathVariable postId: UUID, @RequestParam userId: UUID): Boolean {
        val post = communityPostRepository.findById(postId).orElseThrow { RuntimeException("Post not found") }
        
        if (communityLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            // Unlike
            val like = communityLikeRepository.findAll().find { it.postId == postId && it.userId == userId } // Inefficient but simple for now without custom query
            // Ideally define findByPostIdAndUserId in repo
            // communityLikeRepository.deleteByPostIdAndUserId(postId, userId) needs @Transactional
            
            // Let's rely on finding it first to get ID or just add a proper delete method in repo if Transactional is handled
            // For now, let's just use the findAll approach or add a method to Repo that returns Optional
            
            // Better:
            val likes = communityLikeRepository.findAll()
            val likeToDelete = likes.find { it.postId == postId && it.userId == userId }
            if (likeToDelete != null) {
                communityLikeRepository.delete(likeToDelete)
                post.upvotes = (post.upvotes - 1).coerceAtLeast(0)
                communityPostRepository.save(post)
                return false // Not liked anymore
            }
        } else {
            // Like
            val like = com.rajasthanexams.backend.model.CommunityLike(postId = postId, userId = userId)
            communityLikeRepository.save(like)
            post.upvotes += 1
            communityPostRepository.save(post)
            return true // Liked
        }
        return false
    }
}

