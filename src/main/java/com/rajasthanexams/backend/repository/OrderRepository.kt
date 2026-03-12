package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByRazorpayOrderId(razorpayOrderId: String): Order?
    fun findAllByOrderByCreatedAtDesc(): List<Order>
    fun findAllByStatus(status: String): List<Order>
    fun countByStatus(status: String): Long
}
