package com.salahmate.app.core

import com.salahmate.app.core.util.HijriCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The app uses the arithmetic (tabular) Hijri calendar, which is deterministic and offline but can
 * differ by a day from local moon-sighting announcements. These tests pin the algorithm itself.
 */
class HijriCalendarTest {

    @Test
    fun `known conversion - 1 Ramadan 1445 is 11 March 2024`() {
        val hijri = HijriCalendar.fromGregorian(LocalDate.of(2024, 3, 11))
        assertEquals(1445, hijri.year)
        assertEquals(9, hijri.month)
        assertEquals(1, hijri.day)
    }

    @Test
    fun `known conversion - 1 Ramadan 1446 is 1 March 2025`() {
        val hijri = HijriCalendar.fromGregorian(LocalDate.of(2025, 3, 1))
        assertEquals(1446, hijri.year)
        assertEquals(9, hijri.month)
        assertEquals(1, hijri.day)
    }

    @Test
    fun `hijri to gregorian round trips`() {
        for (year in 1440..1450) {
            for (month in 1..12) {
                val gregorian = HijriCalendar.gregorianFor(year, month, 1)
                val back = HijriCalendar.fromGregorian(gregorian)
                assertEquals("year for $year-$month", year, back.year)
                assertEquals("month for $year-$month", month, back.month)
                assertEquals("day for $year-$month", 1, back.day)
            }
        }
    }

    @Test
    fun `adjustment shifts the date by whole days`() {
        val base = HijriCalendar.fromGregorian(LocalDate.of(2026, 10, 24))
        val plus = HijriCalendar.fromGregorian(LocalDate.of(2026, 10, 24), adjustmentDays = 1)
        assertEquals(base.day + 1, plus.day)
        assertEquals(1, plus.adjustmentDays)
    }

    @Test
    fun `every gregorian day maps to a valid hijri day of month`() {
        var date = LocalDate.of(2026, 1, 1)
        while (date.year == 2026) {
            val hijri = HijriCalendar.fromGregorian(date)
            assertTrue("invalid month for $date", hijri.month in 1..12)
            assertTrue("invalid day for $date", hijri.day in 1..30)
            assertTrue("invalid year for $date", hijri.year > 1400)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `month length is 29 or 30 days`() {
        for (month in 1..12) {
            val length = HijriCalendar.monthLength(1447, month)
            assertTrue("month $month length $length", length in 29..30)
        }
    }
}
