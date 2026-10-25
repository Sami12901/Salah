package com.salahmate.app.domain

import com.salahmate.app.core.db.HifzState
import com.salahmate.app.domain.tracking.HifzScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Review-interval behaviour for the hifz tracker. */
class HifzSchedulerTest {

    private val now: Instant = Instant.parse("2026-10-24T06:00:00Z")

    private fun daysUntilNext(state: String, reviewCount: Int): Long =
        Duration.between(now, HifzScheduler.nextReview(state, reviewCount, now)).toDays()

    @Test
    fun `memorised portions follow the expanding interval`() {
        assertEquals(1L, daysUntilNext(HifzState.MEMORIZED.name, 0))
        assertEquals(3L, daysUntilNext(HifzState.MEMORIZED.name, 1))
        assertEquals(7L, daysUntilNext(HifzState.MEMORIZED.name, 2))
        assertEquals(14L, daysUntilNext(HifzState.MEMORIZED.name, 3))
        assertEquals(30L, daysUntilNext(HifzState.MEMORIZED.name, 4))
        assertEquals(60L, daysUntilNext(HifzState.MEMORIZED.name, 5))
        // Beyond the table the longest interval is reused, never shortened.
        assertEquals(60L, daysUntilNext(HifzState.MEMORIZED.name, 12))
    }

    @Test
    fun `learning and needs-revision are reviewed daily`() {
        assertEquals(1L, daysUntilNext(HifzState.LEARNING.name, 0))
        assertEquals(1L, daysUntilNext(HifzState.LEARNING.name, 4))
        assertEquals(1L, daysUntilNext(HifzState.NEEDS_REVISION.name, 0))
        assertEquals(1L, daysUntilNext(HifzState.NEEDS_REVISION.name, 7))
    }

    @Test
    fun `isDue is true for never-reviewed and past-due items`() {
        assertTrue(HifzScheduler.isDue(null, now))
        assertTrue(HifzScheduler.isDue(now.minusSeconds(60), now))
        assertTrue(HifzScheduler.isDue(now, now))
        assertFalse(HifzScheduler.isDue(now.plusSeconds(60), now))
    }
}
