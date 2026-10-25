package com.salahmate.app.domain

import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Countdown and window logic, tested with fixed instants (no wall-clock dependency). */
class PrayerScheduleTest {

    private val zone = ZoneId.of("Asia/Dhaka")
    private val date = LocalDate.of(2026, 10, 24)

    private fun at(hour: Int, minute: Int = 0): Instant = date.atTime(hour, minute).atZone(zone).toInstant()

    private fun day(
        fajr: Int = 5, dhuhr: Int = 12, asr: Int = 16, maghrib: Int = 18, isha: Int = 19,
        d: LocalDate = date,
    ): DayPrayerTimes = DayPrayerTimes(
        date = d,
        zoneId = zone,
        times = mapOf(
            PrayerKey.FAJR to d.atTime(fajr, 0).atZone(zone).toInstant(),
            PrayerKey.SUNRISE to d.atTime(6, 0).atZone(zone).toInstant(),
            PrayerKey.DHUHR to d.atTime(dhuhr, 0).atZone(zone).toInstant(),
            PrayerKey.ASR to d.atTime(asr, 0).atZone(zone).toInstant(),
            PrayerKey.MAGHRIB to d.atTime(maghrib, 0).atZone(zone).toInstant(),
            PrayerKey.ISHA to d.atTime(isha, 0).atZone(zone).toInstant(),
        ),
    )

    @Test
    fun `before fajr the current prayer is yesterday isha and next is fajr`() {
        val progress = PrayerSchedule.progress(
            today = day(),
            now = at(3, 30),
            yesterday = day(d = date.minusDays(1)),
        )
        assertNotNull(progress)
        assertEquals(PrayerKey.ISHA, progress!!.current)
        assertEquals(PrayerKey.FAJR, progress.next)
        assertEquals(Duration.ofMinutes(90), progress.remaining)
    }

    @Test
    fun `between fajr and dhuhr the next prayer is dhuhr`() {
        val progress = PrayerSchedule.progress(day(), at(9, 0))!!
        assertEquals(PrayerKey.FAJR, progress.current)
        assertEquals(PrayerKey.DHUHR, progress.next)
        assertEquals(Duration.ofHours(3), progress.remaining)
    }

    @Test
    fun `exactly at a prayer time that prayer is current`() {
        val progress = PrayerSchedule.progress(day(), at(12, 0))!!
        assertEquals(PrayerKey.DHUHR, progress.current)
        assertEquals(PrayerKey.ASR, progress.next)
    }

    @Test
    fun `after isha the next prayer is tomorrow fajr`() {
        val tomorrow = day(d = date.plusDays(1))
        val progress = PrayerSchedule.progress(day(), at(22, 0), tomorrow = tomorrow)!!
        assertEquals(PrayerKey.ISHA, progress.current)
        assertEquals(PrayerKey.FAJR, progress.next)
        assertTrue(progress.isNextTomorrow)
        assertEquals(Duration.ofHours(7), progress.remaining)
    }

    @Test
    fun `after isha without tomorrow data still returns a countdown`() {
        val progress = PrayerSchedule.progress(day(), at(22, 0))!!
        assertEquals(PrayerKey.FAJR, progress.next)
        assertTrue(progress.isNextTomorrow)
        assertTrue(progress.remaining.toMillis() > 0)
    }

    @Test
    fun `window progress is between zero and one and increases over the window`() {
        val early = PrayerSchedule.progress(day(), at(12, 30))!!
        val late = PrayerSchedule.progress(day(), at(15, 30))!!
        val earlyFraction = PrayerSchedule.windowProgress(early)
        val lateFraction = PrayerSchedule.windowProgress(late)
        assertTrue(earlyFraction in 0f..1f)
        assertTrue(lateFraction in 0f..1f)
        assertTrue("late window progress should be greater", lateFraction > earlyFraction)
    }

    @Test
    fun `incomplete schedule returns null instead of inventing times`() {
        val incomplete = DayPrayerTimes(
            date = date,
            zoneId = zone,
            times = mapOf(PrayerKey.FAJR to at(5)),
        )
        assertNull(PrayerSchedule.progress(incomplete, at(9, 0)))
    }

    @Test
    fun `countdown never goes negative during a normal day`() {
        // Walk every 7 minutes through a day and assert the remaining time is >= 0 until midnight.
        var cursor = at(0, 0)
        val end = at(23, 50)
        while (cursor.isBefore(end)) {
            val progress = PrayerSchedule.progress(day(), cursor, tomorrow = day(d = date.plusDays(1)))!!
            assertTrue("negative countdown at $cursor", !progress.remaining.isNegative)
            cursor = cursor.plus(Duration.ofMinutes(7))
        }
    }
}
