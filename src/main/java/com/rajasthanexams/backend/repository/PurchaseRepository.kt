package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.Purchase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseRepository : JpaRepository<Purchase, UUID> {
    fun findByUserIdAndExamId(userId: String, examId: String): Purchase?
    fun existsByUserIdAndExamId(userId: String, examId: String): Boolean
    fun findAllByUserId(userId: String): List<Purchase>
}
