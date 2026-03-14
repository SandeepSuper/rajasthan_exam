package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.CreateExamRequest
import com.rajasthanexams.backend.dto.CreateQuestionRequest
import com.rajasthanexams.backend.dto.CreateTestRequest
import com.rajasthanexams.backend.model.AppConfig
import com.rajasthanexams.backend.model.Exam
import com.rajasthanexams.backend.model.Order
import com.rajasthanexams.backend.model.Purchase
import com.rajasthanexams.backend.model.Question
import com.rajasthanexams.backend.model.Test
import com.rajasthanexams.backend.repository.AppConfigRepository
import com.rajasthanexams.backend.repository.CommunityCommentRepository
import com.rajasthanexams.backend.repository.CommunityPostRepository
import com.rajasthanexams.backend.repository.ExamRepository
import com.rajasthanexams.backend.repository.OrderRepository
import com.rajasthanexams.backend.repository.PurchaseRepository
import com.rajasthanexams.backend.repository.UserRepository
import com.rajasthanexams.backend.service.ContentService
import com.rajasthanexams.backend.service.RedisService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Content Management", description = "APIs for creating Exams, Tests, and Questions")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
class AdminController(
    private val contentService: ContentService,
    private val orderRepository: OrderRepository,
    private val purchaseRepository: PurchaseRepository,
    private val examRepository: ExamRepository,
    private val userRepository: UserRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val appConfigRepository: AppConfigRepository,
    private val redisService: RedisService
) {

    @PostMapping("/exams")
    @Operation(summary = "Create Exam", description = "Creates a new Exam category (e.g., REET, CET).")
    fun createExam(@RequestBody request: CreateExamRequest): ResponseEntity<Exam> {
        val exam = contentService.createExam(request)
        return ResponseEntity.ok(exam)
    }

    @PutMapping("/exams/{examId}")
    @Operation(summary = "Update Exam", description = "Updates an existing Exam.")
    fun updateExam(
        @PathVariable examId: UUID,
        @RequestBody request: com.rajasthanexams.backend.dto.UpdateExamRequest
    ): ResponseEntity<Exam> {
        val exam = contentService.updateExam(examId, request)
        return ResponseEntity.ok(exam)
    }

    @PatchMapping("/exams/{examId}/discount")
    @Operation(summary = "Update Exam Discount", description = "Set or update the discount percent for an exam.")
    fun updateDiscount(
        @PathVariable examId: UUID,
        @RequestBody body: Map<String, Int>
    ): ResponseEntity<Exam> {
        val discountPercent = body["discountPercent"] ?: return ResponseEntity.badRequest().build()
        val exam = examRepository.findById(examId).orElseThrow { IllegalArgumentException("Exam not found") }
        val updated = exam.copy(discountPercent = discountPercent.coerceIn(0, 100))
        return ResponseEntity.ok(examRepository.save(updated))
    }

    @PatchMapping("/exams/{examId}/price")
    @Operation(summary = "Update Exam Price", description = "Set or update the price for an exam.")
    fun updatePrice(
        @PathVariable examId: UUID,
        @RequestBody body: Map<String, Double>
    ): ResponseEntity<Exam> {
        val price = body["price"] ?: return ResponseEntity.badRequest().build()
        val exam = examRepository.findById(examId).orElseThrow { IllegalArgumentException("Exam not found") }
        val updated = exam.copy(price = price.coerceAtLeast(0.0))
        return ResponseEntity.ok(examRepository.save(updated))
    }

    @PostMapping("/tests")
    @Operation(summary = "Create Test", description = "Creates a new Test/Mock under an Exam.")
    fun createTest(@RequestBody request: CreateTestRequest): ResponseEntity<Test> {
        val test = contentService.createTest(request)
        return ResponseEntity.ok(test)
    }

    @PostMapping("/questions")
    @Operation(summary = "Create Question", description = "Adds a question to a Test.")
    fun createQuestion(@RequestBody request: CreateQuestionRequest): ResponseEntity<Question> {
        val question = contentService.createQuestion(request)
        return ResponseEntity.ok(question)
    }

    @PostMapping("/tests/{testId}/import")
    @Operation(summary = "Bulk Import Questions", description = "Upload CSV to import questions for a test.")
    fun importQuestions(
        @PathVariable testId: UUID,
        @RequestParam("file") file: org.springframework.web.multipart.MultipartFile
    ): ResponseEntity<String> {
        val count = contentService.importQuestions(testId, file)
        return ResponseEntity.ok("Successfully imported $count questions.")
    }

    // ─── App Config Admin APIs ────────────────────────────────────────────────

    @GetMapping("/config")
    @Operation(summary = "Get App Config", description = "Fetch the current application configuration.")
    fun getAppConfig(): ResponseEntity<AppConfig> {
        val config = appConfigRepository.findById(1L).orElseGet {
            appConfigRepository.save(AppConfig())
        }
        return ResponseEntity.ok(config)
    }

    @PutMapping("/config")
    @Operation(summary = "Update App Config", description = "Updates the application configuration.")
    fun updateAppConfig(@RequestBody newConfig: AppConfig): ResponseEntity<AppConfig> {
        val config = appConfigRepository.findById(1L).orElseGet {
            AppConfig()
        }
        val updated = config.copy(
            playStoreUrl = newConfig.playStoreUrl,
            referrerCoinReward = newConfig.referrerCoinReward,
            refereeCoinReward = newConfig.refereeCoinReward,
            shareMessageTemplate = newConfig.shareMessageTemplate
        )
        return ResponseEntity.ok(appConfigRepository.save(updated))
    }

    // ─── Payment Admin APIs ───────────────────────────────────────────────────

    @PostMapping("/payments/seed-test-data")
    @Operation(summary = "Seed Test Payment Data", description = "Inserts dummy orders for UI testing. Dev use only.")
    fun seedTestPaymentData(): ResponseEntity<Map<String, Any>> {
        return try {
            val exams = examRepository.findAll()
            if (exams.isEmpty()) {
                return ResponseEntity.badRequest().body(mapOf("error" to "No exams found. Create an exam first."))
            }
            val users = userRepository.findAll()
            val dummyUserId = if (users.isNotEmpty()) users.first().id!!.toString() else "00000000-0000-0000-0000-000000000001"

            val statuses = listOf("PAID", "PAID", "PAID", "PAID", "CREATED", "FAILED")
            val amounts  = listOf(299.0, 499.0, 199.0, 399.0, 599.0, 149.0)
            var created = 0

            for (i in statuses.indices) {
                val exam = exams[i % exams.size]
                val razorpayId = "order_TEST_${System.currentTimeMillis()}_$i"
                val paymentId  = if (statuses[i] == "PAID") "pay_TEST_${System.currentTimeMillis()}_$i" else null

                val order = Order(
                    userId          = dummyUserId,
                    examId          = exam.id!!.toString(),
                    razorpayOrderId = razorpayId,
                    amount          = amounts[i],
                    status          = statuses[i],
                    paymentId       = paymentId
                )
                orderRepository.save(order)

                // Create a purchase record for PAID orders
                if (statuses[i] == "PAID") {
                    val purchase = Purchase(
                        userId  = dummyUserId,
                        examId  = exam.id.toString()
                    )
                    purchaseRepository.save(purchase)
                }
                created++
            }

            ResponseEntity.ok(mapOf(
                "message" to "Successfully created $created test orders.",
                "paidOrders"    to statuses.count { it == "PAID" },
                "pendingOrders" to statuses.count { it == "CREATED" },
                "failedOrders"  to statuses.count { it == "FAILED" }
            ))
        } catch (e: Exception) {
            println("seedTestPaymentData error: ${e.message}")
            e.printStackTrace()
            ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    @GetMapping("/payments/stats")
    @Operation(summary = "Payment Stats")
    fun paymentStats(): ResponseEntity<Map<String, Any>> {
        return try {
            val allOrders   = orderRepository.findAll()
            val paidOrders  = allOrders.filter { it.status == "PAID" }
            val totalRevenue = paidOrders.sumOf { it.amount }

            // Build revenue-by-exam safely using a plain loop
            val revenueByExam = mutableMapOf<String, Double>()
            for (order in paidOrders) {
                val examTitle = try {
                    val uuid = UUID.fromString(order.examId)
                    val opt  = examRepository.findById(uuid)
                    if (opt.isPresent) opt.get().title else order.examId
                } catch (e: Exception) { order.examId }
                revenueByExam[examTitle] = (revenueByExam[examTitle] ?: 0.0) + order.amount
            }

            ResponseEntity.ok(mapOf(
                "totalRevenue"   to totalRevenue,
                "totalOrders"    to allOrders.size,
                "paidOrders"     to paidOrders.size,
                "pendingOrders"  to allOrders.count { it.status == "CREATED" },
                "failedOrders"   to allOrders.count { it.status == "FAILED" },
                "totalPurchases" to purchaseRepository.count(),
                "totalUsers"     to userRepository.count(),
                "revenueByExam"  to revenueByExam
            ))
        } catch (e: Exception) {
            println("paymentStats error: ${e.message}")
            e.printStackTrace()
            ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    @GetMapping("/payments/orders")
    @Operation(summary = "All Orders")
    fun allOrders(): ResponseEntity<List<Map<String, Any?>>> {
        return try {
            val orders = orderRepository.findAllByOrderByCreatedAtDesc()
            val result = mutableListOf<Map<String, Any?>>()
            for ((index, order) in orders.withIndex()) {
                val examTitle = try {
                    val uuid = UUID.fromString(order.examId)
                    val opt  = examRepository.findById(uuid)
                    if (opt.isPresent) opt.get().title else order.examId
                } catch (e: Exception) { order.examId }

                val userName = try {
                    val uuid = UUID.fromString(order.userId)
                    val opt  = userRepository.findById(uuid)
                    if (opt.isPresent) (opt.get().name ?: opt.get().mobile) else order.userId
                } catch (e: Exception) { order.userId }

                result.add(mapOf(
                    "id"              to order.id,
                    "examTitle"       to examTitle,
                    "userName"        to userName,
                    "amount"          to order.amount,
                    "status"          to order.status,
                    "razorpayOrderId" to order.razorpayOrderId,
                    "paymentId"       to order.paymentId,
                    "createdAt"       to order.createdAt.toString()
                ))
            }
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            println("allOrders error: ${e.message}")
            e.printStackTrace()
            ResponseEntity.internalServerError().body(listOf(mapOf("error" to (e.message ?: "Unknown error"))))
        }
    }

    @GetMapping("/payments/exams")
    @Operation(summary = "Exams with Discount Info")
    fun examsForDiscount(): ResponseEntity<List<Map<String, Any?>>> {
        return try {
            val result = mutableListOf<Map<String, Any?>>()
            for (exam in examRepository.findAll()) {
                result.add(mapOf(
                    "id"              to exam.id,
                    "title"           to exam.title,
                    "category"        to exam.category,
                    "price"           to (exam.price ?: 0.0),
                    "discountPercent" to (exam.discountPercent ?: 0),
                    "isPremium"       to (exam.isPremium ?: false)
                ))
            }
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            println("examsForDiscount error: ${e.message}")
            e.printStackTrace()
            ResponseEntity.internalServerError().body(listOf(mapOf("error" to (e.message ?: "Unknown error"))))
        }
    }

    @GetMapping("/content/overview")
    @Operation(summary = "Content Overview", description = "Returns all exams with their tests, grouped by category.")
    fun contentOverview(): ResponseEntity<Map<String, Any>> {
        return try {
            val allExams = examRepository.findAll()
            val examList = mutableListOf<Map<String, Any?>>()
            var totalTests = 0
            var totalQuestions = 0
            val categoryCounts = mutableMapOf<String, Int>()

            for (exam in allExams) {
                val tests = contentService.getTests(exam.id!!)
                val qCount = tests.sumOf { test -> contentService.getQuestions(test.id!!).size }
                totalTests += tests.size
                totalQuestions += qCount
                categoryCounts[exam.category] = (categoryCounts[exam.category] ?: 0) + 1

                examList.add(mapOf(
                    "id"              to exam.id,
                    "title"           to exam.title,
                    "category"        to exam.category,
                    "isPremium"       to (exam.isPremium ?: false),
                    "price"           to (exam.price ?: 0.0),
                    "discountPercent" to (exam.discountPercent ?: 0),
                    "testCount"       to tests.size,
                    "questionCount"   to qCount,
                    "tests"           to tests.map { t ->
                        mapOf(
                            "id"              to t.id,
                            "title"           to t.title,
                            "type"            to t.type.name,
                            "durationMinutes" to t.durationMinutes,
                            "totalQuestions"  to t.totalQuestions,
                            "isPremium"       to t.isPremium,
                            "isLive"          to (t.isLive ?: false)
                        )
                    }
                ))
            }

            ResponseEntity.ok(mapOf(
                "totalExams"     to allExams.size,
                "totalTests"     to totalTests,
                "totalQuestions" to totalQuestions,
                "categories"     to categoryCounts.keys.size,
                "categoryCounts" to categoryCounts,
                "exams"          to examList
            ))
        } catch (e: Exception) {
            println("contentOverview error: ${e.message}")
            e.printStackTrace()
            ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // ─── Community Admin APIs ─────────────────────────────────────────────────

    @GetMapping("/community/stats")
    @Operation(summary = "Community Stats")
    fun communityStats(): ResponseEntity<Map<String, Any>> {
        val posts = communityPostRepository.findAll()
        val totalComments = communityCommentRepository.count()
        val verifiedAnswers = posts.count { !it.verifiedAnswer.isNullOrBlank() }
        val totalUpvotes = posts.sumOf { it.upvotes }
        return ResponseEntity.ok(mapOf(
            "totalPosts" to posts.size,
            "totalComments" to totalComments,
            "verifiedAnswers" to verifiedAnswers,
            "totalUpvotes" to totalUpvotes
        ))
    }

    @DeleteMapping("/community/posts/{postId}")
    @Operation(summary = "Delete Community Post")
    fun deletePost(@PathVariable postId: UUID): ResponseEntity<Void> {
        if (!communityPostRepository.existsById(postId)) return ResponseEntity.notFound().build()
        val comments = communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId)
        communityCommentRepository.deleteAll(comments)
        communityPostRepository.deleteById(postId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/community/comments/{commentId}")
    @Operation(summary = "Delete Community Comment")
    fun deleteComment(@PathVariable commentId: UUID): ResponseEntity<Void> {
        val comment = communityCommentRepository.findById(commentId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val post = communityPostRepository.findById(comment.postId).orElse(null)
        if (post != null) {
            post.commentCount = (post.commentCount - 1).coerceAtLeast(0)
            communityPostRepository.save(post)
        }
        communityCommentRepository.deleteById(commentId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/community/posts/{postId}/verify")
    @Operation(summary = "Add Verified Answer to Post")
    fun verifyPost(
        @PathVariable postId: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, Any?>> {
        val answer = body["answer"]?.trim()
        if (answer.isNullOrBlank()) return ResponseEntity.badRequest().build()
        val post = communityPostRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        communityPostRepository.save(post.copy(verifiedAnswer = answer))
        return ResponseEntity.ok(mapOf("postId" to postId, "verifiedAnswer" to answer))
    }

    // ─── Community User Block / Unblock ──────────────────────────────────────

    @PutMapping("/community/users/{userId}/block")
    @Operation(summary = "Block user from community")
    fun blockUser(@PathVariable userId: UUID): ResponseEntity<Map<String, Any?>> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        user.isCommunityBlocked = true
        userRepository.save(user)
        return ResponseEntity.ok(mapOf(
            "userId" to userId,
            "isCommunityBlocked" to true,
            "message" to "${user.name ?: user.mobile} is now blocked from community"
        ))
    }

    @PutMapping("/community/users/{userId}/unblock")
    @Operation(summary = "Unblock user from community")
    fun unblockUser(@PathVariable userId: UUID): ResponseEntity<Map<String, Any?>> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        user.isCommunityBlocked = false
        userRepository.save(user)
        return ResponseEntity.ok(mapOf(
            "userId" to userId,
            "isCommunityBlocked" to false,
            "message" to "${user.name ?: user.mobile} is now unblocked"
        ))
    }

    @GetMapping("/community/users/blocked")
    @Operation(summary = "List all blocked users")
    fun getBlockedUsers(): ResponseEntity<List<Map<String, Any?>>> {
        val blocked = userRepository.findByIsCommunityBlocked(true)
        return ResponseEntity.ok(blocked.map { u ->
            mapOf(
                "userId" to u.id,
                "name" to (u.name ?: "—"),
                "mobile" to u.mobile,
                "isCommunityBlocked" to u.isCommunityBlocked
            )
        })
    }

    // ─── Rate Limit Management ────────────────────────────────────────────────

    @GetMapping("/community/rate-limit/{userId}")
    @Operation(summary = "Get rate limit usage for a user")
    fun getRateLimitStatus(@PathVariable userId: String): ResponseEntity<Map<String, Any?>> {
        val dailyKey  = "rate:community:post:daily:$userId"
        val minuteKey = "rate:community:post:minute:$userId"

        val dailyCount  = redisService.getCounter(dailyKey)
        val minuteCount = redisService.getCounter(minuteKey)
        val dailyTtl    = redisService.getTtlSeconds(dailyKey)
        val minuteTtl   = redisService.getTtlSeconds(minuteKey)

        // Also lookup user info
        val user = try { userRepository.findById(java.util.UUID.fromString(userId)).orElse(null) } catch (e: Exception) { null }

        return ResponseEntity.ok(mapOf(
            "userId"         to userId,
            "userName"       to (user?.name ?: "—"),
            "mobile"         to (user?.mobile ?: "—"),
            "dailyPosts"     to dailyCount,
            "dailyLimit"     to 30,
            "dailyTtlSec"    to dailyTtl,
            "minutePosts"    to minuteCount,
            "minuteLimit"    to 5,
            "minuteTtlSec"   to minuteTtl
        ))
    }

    @DeleteMapping("/community/rate-limit/{userId}/daily")
    @Operation(summary = "Reset daily post limit for a user")
    fun resetDailyLimit(@PathVariable userId: String): ResponseEntity<Map<String, Any?>> {
        redisService.deleteKey("rate:community:post:daily:$userId")
        return ResponseEntity.ok(mapOf("message" to "Daily rate limit reset for user $userId"))
    }

    @DeleteMapping("/community/rate-limit/{userId}/minute")
    @Operation(summary = "Reset per-minute post limit for a user")
    fun resetMinuteLimit(@PathVariable userId: String): ResponseEntity<Map<String, Any?>> {
        redisService.deleteKey("rate:community:post:minute:$userId")
        return ResponseEntity.ok(mapOf("message" to "Minute rate limit reset for user $userId"))
    }

    // ─── User Search ──────────────────────────────────────────────────────────

    @GetMapping("/users/search")
    @Operation(summary = "Search Users", description = "Find users by name or mobile number.")
    fun searchUsers(@RequestParam q: String): ResponseEntity<List<Map<String, Any?>>> {
        val users = userRepository.searchUsers(q).take(20) // Limit to top 20
        return ResponseEntity.ok(users.map { u ->
            mapOf(
                "id" to u.id,
                "name" to (u.name ?: "—"),
                "mobile" to u.mobile,
                "coins" to (u.coins ?: 0)
            )
        })
    }
}
