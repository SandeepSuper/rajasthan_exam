package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.BookmarkRequest
import com.rajasthanexams.backend.model.Bookmark
import com.rajasthanexams.backend.model.EntityType
import com.rajasthanexams.backend.model.TestAttempt
import com.rajasthanexams.backend.repository.BookmarkRepository
import com.rajasthanexams.backend.repository.TestAttemptRepository
import com.rajasthanexams.backend.repository.UserRepository
import com.rajasthanexams.backend.service.JwtService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "User specific data like bookmarks and history")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
class UserController(
    private val bookmarkRepository: BookmarkRepository,
    private val testAttemptRepository: TestAttemptRepository,
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    @PostMapping("/bookmarks")
    @Operation(summary = "Add Bookmark", description = "Bookmarks a Question or News Item.")
    fun addBookmark(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: BookmarkRequest
    ): ResponseEntity<String> {
        val user = getUserFromToken(authHeader)
        val bookmark = Bookmark(
            user = user,
            entityType = request.entityType,
            entityId = request.entityId
        )
        bookmarkRepository.save(bookmark)
        return ResponseEntity.ok("Bookmarked successfully")
    }

    @DeleteMapping("/bookmarks")
    @Transactional
    @Operation(summary = "Remove Bookmark", description = "Removes a bookmark.")
    fun removeBookmark(
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam entityType: EntityType,
        @RequestParam entityId: UUID
    ): ResponseEntity<String> {
        val user = getUserFromToken(authHeader)
        bookmarkRepository.deleteByUserIdAndEntityTypeAndEntityId(user.id!!, entityType, entityId)
        return ResponseEntity.ok("Removed bookmark")
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "Get Bookmarks", description = "List all bookmarks for the user.")
    fun getBookmarks(@RequestHeader("Authorization") authHeader: String): ResponseEntity<List<Bookmark>> {
        val user = getUserFromToken(authHeader)
        val bookmarks = bookmarkRepository.findByUserId(user.id!!)
        return ResponseEntity.ok(bookmarks)
    }

    @GetMapping("/profile")
    @Operation(summary = "Get Profile", description = "Returns the latest profile and coin balance of the user.")
    fun getProfile(@RequestHeader("Authorization") authHeader: String): ResponseEntity<com.rajasthanexams.backend.dto.UserProfileResponse> {
        val user = getUserFromToken(authHeader)
        val response = com.rajasthanexams.backend.dto.UserProfileResponse(
            id = user.id.toString(),
            name = user.name,
            email = user.email,
            mobile = user.mobile,
            profilePicture = user.profilePicture,
            coins = user.coins ?: 0,
            referCode = user.referCode,
            isPremium = user.isPremium
        )
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/profile/mobile")
    @Operation(summary = "Update Mobile", description = "Saves or updates the user's mobile number (optional).")
    fun updateMobile(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody body: Map<String, String?>
    ): ResponseEntity<com.rajasthanexams.backend.dto.ApiResponse> {
        val user = getUserFromToken(authHeader)
        user.mobile = body["mobile"]
        userRepository.save(user)
        return ResponseEntity.ok(com.rajasthanexams.backend.dto.ApiResponse(message = "Mobile updated"))
    }

    @GetMapping("/history")
    @Operation(summary = "Get Test History", description = "List all past test attempts.")
    fun getTestHistory(@RequestHeader("Authorization") authHeader: String): ResponseEntity<List<TestAttempt>> {
        val user = getUserFromToken(authHeader)
        val history = testAttemptRepository.findByUserIdOrderByAttemptDateDesc(user.id!!)
        return ResponseEntity.ok(history)
    }

    private fun getUserFromToken(authHeader: String): com.rajasthanexams.backend.model.User {
        val token = authHeader.substring(7)
        val subject = jwtService.extractUsername(token)
        // Try email first (new email-auth users), then mobile (legacy OTP users)
        return userRepository.findByEmail(subject)
            .orElseGet { userRepository.findByMobile(subject).orElse(null) }
            ?: throw IllegalArgumentException("User not found")
    }
}
