package com.salahmate.app.domain.tracking

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Spaced-repetition schedule for Hifz review, deliberately simple and explainable:
 * a memorised portion is revised after 1, 3, 7, 14, 30 then every 60 days; a portion that needs
 * revision comes back the next day. No machine learning, no hidden scoring.
 */
object HifzScheduler {

    private val intervals = listOf(1L, 3L, 7L, 14L, 30L, 60L)

    fun nextReview(state: String, reviewCount: Int, reviewedAt: Instant): Instant = when (state) {
        "MEMORIZED" -> {
            val index = reviewCount.coerceIn(0, intervals.size - 1)
            reviewedAt.plus(intervals[index], ChronoUnit.DAYS)
        }
        "LEARNING" -> reviewedAt.plus(1, ChronoUnit.DAYS)
        "NEEDS_REVISION" -> reviewedAt.plus(1, ChronoUnit.DAYS)
        else -> reviewedAt.plus(1, ChronoUnit.DAYS)
    }

    fun isDue(nextReviewAt: Instant?, now: Instant): Boolean =
        nextReviewAt == null || !nextReviewAt.isAfter(now)
}
