package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.model.Order
import com.rajasthanexams.backend.model.Purchase
import com.rajasthanexams.backend.repository.ExamRepository
import com.rajasthanexams.backend.repository.OrderRepository
import com.rajasthanexams.backend.repository.PurchaseRepository
import com.rajasthanexams.backend.repository.UserRepository
import com.razorpay.RazorpayClient
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min
import kotlin.math.round

@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val orderRepository: OrderRepository,
    private val purchaseRepository: PurchaseRepository,
    private val examRepository: ExamRepository,
    private val userRepository: UserRepository
) {

    @Value("\${razorpay.key.id:rzp_test_KEY}")
    private lateinit var keyId: String

    @Value("\${razorpay.key.secret:rzp_test_SECRET}")
    private lateinit var keySecret: String

    @Value("\${app.coin-value:0.10}")
    private var coinValue: Double = 0.10

    @Value("\${app.max-coin-discount-percent:10}")
    private var maxCoinDiscountPercent: Int = 10

    /** Typed request body for create-order */
    data class CreateOrderRequest(
        val examId: String,
        val useCoins: Boolean = false
    )

    /** Preview how many coins can be used and what discount they give */
    @GetMapping("/coin-preview")
    fun getCoinPreview(
        @RequestParam examId: String,
        principal: Principal
    ): ResponseEntity<Any> {
        val user = userRepository.findByMobile(principal.name).orElse(null)
            ?: return ResponseEntity.badRequest().body("User not found")
        val exam = examRepository.findById(UUID.fromString(examId)).orElse(null)
            ?: return ResponseEntity.badRequest().body("Exam not found")

        val basePrice = getDiscountedPrice(exam.price ?: 0.0, exam.discountPercent ?: 0)
        val maxDiscountAmount = round(basePrice * maxCoinDiscountPercent / 100.0 * 100) / 100.0
        val maxUsableCoins = min(
            (user.coins ?: 0),
            (maxDiscountAmount / coinValue).toInt()
        )
        val discountAmount = round(maxUsableCoins * coinValue * 100) / 100.0
        val finalPrice = maxOf(0.0, basePrice - discountAmount)

        return ResponseEntity.ok(mapOf(
            "userCoins" to (user.coins ?: 0),
            "coinValue" to coinValue,
            "maxUsableCoins" to maxUsableCoins,
            "discountAmount" to discountAmount,
            "basePrice" to basePrice,
            "finalPrice" to finalPrice,
            "maxCoinDiscountPercent" to maxCoinDiscountPercent
        ))
    }

    @PostMapping("/create-order")
    fun createOrder(
        @RequestBody request: CreateOrderRequest,
        principal: Principal
    ): ResponseEntity<Any> {
        try {
            val mobile = principal.name
            val user = userRepository.findByMobile(mobile).orElse(null)
                ?: return ResponseEntity.badRequest().body("User not found")

            val examId = request.examId
            val useCoins = request.useCoins

            val examUUID = try { UUID.fromString(examId) }
            catch (e: IllegalArgumentException) { return ResponseEntity.badRequest().body("Invalid Exam ID format") }

            val exam = examRepository.findById(examUUID).orElse(null)
                ?: return ResponseEntity.badRequest().body("Exam not found")

            if (purchaseRepository.existsByUserIdAndExamId(user.id.toString(), examId)) {
                return ResponseEntity.badRequest().body("Exam already purchased")
            }

            val basePrice = getDiscountedPrice(exam.price ?: 0.0, exam.discountPercent ?: 0)

            // ── Coin discount logic ──────────────────────────────────────────
            var coinsToDeduct = 0
            var coinDiscountAmount = 0.0

            if (useCoins) {
                val maxDiscountAmount = round(basePrice * maxCoinDiscountPercent / 100.0 * 100) / 100.0
                val maxUsableCoins = min(
                    (user.coins ?: 0),
                    (maxDiscountAmount / coinValue).toInt()
                )
                coinsToDeduct = maxUsableCoins
                coinDiscountAmount = round(coinsToDeduct * coinValue * 100) / 100.0
            }

            val finalPrice = maxOf(0.0, basePrice - coinDiscountAmount)
            val amountInPaise = (finalPrice * 100).toInt()

            // ── If coins cover 100% → grant access directly (no Razorpay) ──
            if (amountInPaise == 0 && coinsToDeduct > 0) {
                user.coins = (user.coins ?: 0) - coinsToDeduct
                userRepository.save(user)
                purchaseRepository.save(Purchase(userId = user.id!!.toString(), examId = examId))
                return ResponseEntity.ok(mapOf(
                    "free" to true,
                    "success" to true,
                    "message" to "Exam unlocked with coins!",
                    "coinsUsed" to coinsToDeduct
                ))
            }

            if (amountInPaise < 100) {
                return ResponseEntity.badRequest().body("Price must be at least ₹1 for online payment")
            }

            val client = RazorpayClient(keyId, keySecret)
            val orderRequest = JSONObject()
            orderRequest.put("amount", amountInPaise)
            orderRequest.put("currency", "INR")
            orderRequest.put("receipt", "txn_${System.currentTimeMillis()}")

            val razorpayOrder = client.orders.create(orderRequest)
            val orderId = razorpayOrder.get<String>("id")

            // Store how many coins are reserved for this order
            val order = Order(
                userId = user.id!!.toString(),
                examId = examId,
                razorpayOrderId = orderId,
                amount = finalPrice,
                status = "CREATED",
                coinsUsed = coinsToDeduct   // ← new field
            )
            orderRepository.save(order)

            return ResponseEntity.ok(mapOf(
                "free" to false,
                "orderId" to orderId,
                "amount" to amountInPaise,
                "key" to keyId,
                "examName" to exam.title,
                "description" to "Purchase ${exam.title}",
                "coinsUsed" to coinsToDeduct,
                "coinDiscount" to coinDiscountAmount
            ))

        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.internalServerError().body("Error creating order: ${e.message}")
        }
    }

    @PostMapping("/verify")
    fun verifyPayment(
        @RequestBody data: Map<String, String>,
        principal: Principal
    ): ResponseEntity<Any> {
        val user = userRepository.findByMobile(principal.name).orElse(null)
            ?: return ResponseEntity.badRequest().body("User not found")

        val orderId = data["razorpay_order_id"]
        val paymentId = data["razorpay_payment_id"]
        val signature = data["razorpay_signature"]

        if (orderId == null || paymentId == null || signature == null)
            return ResponseEntity.badRequest().body("Missing payment details")

        val generatedSignature = calculateRFC2104HMAC("$orderId|$paymentId", keySecret)

        if (generatedSignature == signature) {
            val order = orderRepository.findByRazorpayOrderId(orderId)
                ?: return ResponseEntity.badRequest().body("Order not found")

            orderRepository.save(order.copy(status = "PAID", paymentId = paymentId))

            // Deduct coins that were reserved for this order
            if (order.coinsUsed > 0) {
                user.coins = maxOf(0, (user.coins ?: 0) - order.coinsUsed)
                userRepository.save(user)
            }

            purchaseRepository.save(Purchase(userId = user.id!!.toString(), examId = order.examId))
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Payment verified",
                "coinsDeducted" to order.coinsUsed
            ))
        } else {
            return ResponseEntity.badRequest().body("Invalid signature")
        }
    }

    private fun getDiscountedPrice(originalPrice: Double, discountPercent: Int): Double {
        return if (discountPercent > 0)
            round(originalPrice * (1.0 - discountPercent / 100.0) * 100) / 100.0
        else originalPrice
    }

    private fun calculateRFC2104HMAC(data: String, secret: String): String {
        val result = StringBuilder()
        try {
            val signingKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(signingKey)
            val rawHmac = mac.doFinal(data.toByteArray())
            for (b in rawHmac) result.append(String.format("%02x", b))
        } catch (e: Exception) {
            throw RuntimeException("Failed to calculate HMAC: ${e.message}")
        }
        return result.toString()
    }
}
