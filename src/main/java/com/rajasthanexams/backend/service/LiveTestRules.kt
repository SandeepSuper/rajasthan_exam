package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.model.Test
import java.time.LocalDateTime

/**
 * Rules that keep live tests fair.
 *
 * While a live test is open, the questions API does not send correct answers or solutions
 * (otherwise anyone could read them from the network traffic and top the leaderboard).
 * Only a user's first submission made inside the live window counts for rank and coins.
 */
object LiveTestRules {
    /** Sent in place of correctOptionIndex while answers are hidden. */
    const val HIDDEN_ANSWER = -1

    /** Late submissions within this many seconds of the end still count (network delay, auto-submit). */
    private const val SUBMIT_GRACE_SECONDS = 120L

    /** True while a live test is upcoming or running, i.e. answers must stay hidden. */
    fun isAnswerWindowOpen(test: Test, now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (test.isLive != true) return false
        val end = test.endsAt ?: return true
        return now.isBefore(end)
    }

    /** True if a submission made now can count for the live leaderboard. */
    fun isWithinRankedWindow(test: Test, now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (test.isLive != true) return false
        val start = test.startsAt
        val end = test.endsAt
        if (start != null && now.isBefore(start)) return false
        if (end != null && now.isAfter(end.plusSeconds(SUBMIT_GRACE_SECONDS))) return false
        return true
    }
}
