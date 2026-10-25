package com.salahmate.app.core

import com.salahmate.app.core.util.HijriCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Month lengths and localised month names for the Islamic calendar screen. */
class HijriMonthTest {

    @Test
    fun `every month of 1448 is 29 or 30 days`() {
        (1..12).forEach { month ->
            val length = HijriCalendar.monthLength(1448, month)
            assertTrue("month $month length $length", length in 29..30)
        }
    }

    @Test
    fun `twelve months sum to a 354 or 355 day year`() {
        val total = (1..12).sumOf { HijriCalendar.monthLength(1448, it) }
        assertTrue("year length $total", total in 354..355)
    }

    @Test
    fun `month first days are strictly increasing`() {
        val firsts = (1..12).map { HijriCalendar.gregorianFor(1448, it, 1) }
        firsts.zipWithNext().forEach { (a, b) -> assertTrue("$a < $b", a.isBefore(b)) }
    }

    @Test
    fun `month names are localised`() {
        assertEquals("Ramadan", HijriCalendar.monthName(9, bangla = false))
        assertEquals("রমজান", HijriCalendar.monthName(9, bangla = true))
        assertEquals("Muharram", HijriCalendar.monthName(1, bangla = false))
        // Out-of-range input is clamped rather than throwing.
        assertEquals("Dhu al-Hijjah", HijriCalendar.monthName(13, bangla = false))
    }

    @Test
    fun `hijri year 1448 starts in the expected gregorian window`() {
        val newYear = HijriCalendar.gregorianFor(1448, 1, 1)
        assertTrue("1448-01-01 was $newYear", newYear.year == 2026)
    }
}
