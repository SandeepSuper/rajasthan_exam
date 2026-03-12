package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.TestAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TestAttemptRepository : JpaRepository<TestAttempt, UUID> {
    fun findByUserIdOrderByAttemptDateDesc(userId: UUID): List<TestAttempt>
    fun existsByTestIdAndUserId(testId: UUID, userId: UUID): Boolean

    @org.springframework.data.jpa.repository.Query("SELECT t.test.id FROM TestAttempt t WHERE t.user.id = :userId")
    fun findAttemptedTestIdsByUserId(userId: UUID): List<UUID>

    // Fetch top attempts per user for a specific test
    // Strategy: We want the MAX score and MIN time for each user.
    // Since JPA Group By with object selection is tricky, we'll fetch the best attempt IDs first or use a native query.
    // For simplicity and standard JPA, let's use a constructor expression if possible, or fetch all and filter in memory if volume is low.
    // However, for a leaderboard, a native query is often most efficient.
    
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM test_attempts t " +
                "WHERE t.test_id = :testId " +
                "AND (t.score, t.time_taken) IN (" +
                "    SELECT MAX(t2.score), MIN(t2.time_taken) " +
                "    FROM test_attempts t2 " +
                "    WHERE t2.test_id = :testId " +
                "    GROUP BY t2.user_id" +
                ") " +
                "ORDER BY t.score DESC, t.time_taken ASC " +
                "LIMIT 50",
        nativeQuery = true
    )
    fun findTopAttemptsByTestId(testId: UUID): List<TestAttempt>
    
    // Alternative simpler approach: Fetch ALL attempts for test, then distinct by user in memory (Service layer)
    // But let's try a optimized JPQL/Native approach. 
    // Actually, "Greatest N per group" is classic SQL problem.
    // Let's use a simpler approach: Select all attempts for the test, order by score desc, time asc.
    // Then in Service, we pick the first occurrence of each user (which is their best due to sorting).
    // This is safer for JPA and avoids complex nested queries if DB support varies.
    
    /** Fetch all attempts for a test with user and test eagerly loaded (avoids LazyInitializationException) */
    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM TestAttempt a " +
        "JOIN FETCH a.user " +
        "JOIN FETCH a.test " +
        "WHERE a.test.id = :testId " +
        "ORDER BY a.score DESC, a.timeTakenSeconds ASC"
    )
    fun findByTestIdOrderByScoreDescTimeTakenSecondsAsc(testId: UUID): List<TestAttempt>

    /** Fetch ALL attempts (for global leaderboard) with user and test eagerly loaded */
    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM TestAttempt a JOIN FETCH a.user JOIN FETCH a.test"
    )
    override fun findAll(): List<TestAttempt>
}
