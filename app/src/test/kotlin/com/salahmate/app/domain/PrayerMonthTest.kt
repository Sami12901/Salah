package com.salahmate.app.domain

import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerMonth
import com.salahmate.app.domain.prayer.PrayerSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Monthly timetable helper: date grids, month shifting and the partial-day rule. */
class PrayerMonthTest {

    private val dhaka: ZoneId = ZoneId.of("Asia/Dhaka")
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    private fun day(date: LocalDate, complete: Boolean = true): DayPrayerTimes {
        val base = date.atStartOfDay(dhaka).toInstant()
        val times = if (complete) {
            mapOf(
                PrayerKey.FAJR to base.plusSeconds(4 * 3600L + 44 * 60),
                PrayerKey.SUNRISE to base.plusSeconds(6 * 3600L),
                PrayerKey.DHUHR to base.plusSeconds(11 * 3600L + 43 * 60),
                PrayerKey.ASR to base.plusSeconds(15 * 3600L + 49 * 60),
                PrayerKey.MAGHRIB to base.plusSeconds(17 * 3600L + 25 * 60),
                PrayerKey.ISHA to base.plusSeconds(18 * 3600L + 41 * 60),
            )
        } else {
            mapOf(
                PrayerKey.DHUHR to base.plusSeconds(11 * 3600L + 43 * 60),
                PrayerKey.MAGHRIB to base.plusSeconds(17 * 3600L + 25 * 60),
            )
        }
        return DayPrayerTimes(
            date = date,
            zoneId = dhaka,
            times = times,
            source = PrayerSource.LOCAL_ADHAN,
        )
    }

    @Test
    fun `month grids follow the calendar including leap years`() {
        assertEquals(31, PrayerMonth.datesIn(2026, 10).size)
        assertEquals(28, PrayerMonth.datesIn(2026, 2).size)
        assertEquals(29, PrayerMonth.datesIn(2028, 2).size) // 2028 is a leap year
        assertEquals(LocalDate.of(2026, 10, 1), PrayerMonth.datesIn(2026, 10).first())
        assertEquals(LocalDate.of(2026, 10, 31), PrayerMonth.datesIn(2026, 10).last())
    }

    @Test
    fun `month shifting wraps across year boundaries in both directions`() {
        assertEquals(2026 to 11, PrayerMonth.shift(2026, 10, 1))
        assertEquals(2026 to 12, PrayerMonth.shift(2026, 11, 1))
        assertEquals(2027 to 1, PrayerMonth.shift(2026, 12, 1))
        assertEquals(2026 to 1, PrayerMonth.shift(2026, 2, -1))
        assertEquals(2025 to 12, PrayerMonth.shift(2026, 1, -1))
        assertEquals(2026 to 10, PrayerMonth.shift(2026, 10, 0))
    }

    @Test
    fun `rows carry formatted times in the requested zone`() {
        val rows = PrayerMonth.build(listOf(day(LocalDate.of(2026, 10, 24))), dhaka, 0, formatter)
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals("04:44", row.fajr)
        assertEquals("06:00", row.sunrise)
        assertEquals("11:43", row.dhuhr)
        assertEquals("15:49", row.asr)
        assertEquals("17:25", row.maghrib)
        assertEquals("18:41", row.isha)
        assertTrue(row.complete)
        // 24 Oct 2026 is 12 Jumada al-Ula 1448 in the tabular calendar used by the app
        // (same value pinned by HijriCalendarTest).
        assertEquals(12, row.hijriDay)
        assertEquals(5, row.hijriMonth)
    }

    @Test
    fun `incomplete days keep their row but are flagged`() {
        val rows = PrayerMonth.build(
            days = listOf(day(LocalDate.of(2026, 10, 24), complete = false)),
            zone = dhaka,
            hijriOffset = 0,
            formatter = formatter,
        )
        val row = rows.first()
        assertFalse(row.complete)
        assertEquals(null, row.fajr)
        assertEquals("11:43", row.dhuhr)
    }

    @Test
    fun `missing dates reports gaps against the full month`() {
        val month = 10
        val year = 2026
        val rows = PrayerMonth.build(
            days = listOf(day(LocalDate.of(year, month, 1)), day(LocalDate.of(year, month, 31), complete = false)),
            zone = dhaka,
            hijriOffset = 0,
            formatter = formatter,
        )
        val missing = PrayerMonth.missingDates(rows, year, month)
        // Day 1 is complete; day 31 is incomplete; everything else was never supplied.
        assertEquals(30, missing.size)
        assertFalse(missing.contains(LocalDate.of(year, month, 1)))
        assertTrue(missing.contains(LocalDate.of(year, month, 31)))
    }

    @Test
    fun `hijri offset shifts the displayed hijri day`() {
        val date = LocalDate.of(2026, 10, 24)
        val plain = PrayerMonth.build(listOf(day(date)), dhaka, 0, formatter).first()
        val shifted = PrayerMonth.build(listOf(day(date)), dhaka, 2, formatter).first()
        assertEquals(plain.hijriDay + 2, shifted.hijriDay)
    }
}
