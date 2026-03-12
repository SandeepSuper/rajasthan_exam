package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "exam_id", nullable = false)
    val examId: String,

    @Column(name = "razorpay_order_id", nullable = false)
    val razorpayOrderId: String,

    @Column(nullable = false)
    val amount: Double,

    @Column(nullable = false)
    val status: String, // CREATED, PAID, FAILED

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "payment_id")
    val paymentId: String? = null,

    /** Coins reserved for this order — deducted only after payment is verified */
    @Column(name = "coins_used")
    val coinsUsed: Int = 0
)
