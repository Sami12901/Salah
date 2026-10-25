package com.salahmate.app.domain.prayer

import com.salahmate.app.core.util.HijriCalendar
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * One row of the monthly prayer timetable.
 *
 * Times are pre-formatted strings because the row is a display model: the screen must never
 * re-derive a time from a stale zone or a different formatter than the header advertises.
 */
data class TimetableRow(
    val date: LocalDate,
    val hijriDay: Int,
    val hijriMonth: Int,
    val fajr: String?,
    val sunrise: String?,
    val dhuhr: String?,
    val asr: String?,
    val maghrib: String?,
    val isha: String?,
    /** True only when all five obligatory prayers are present — partial days are shown as gaps. */
    val complete: Boolean,
)

/**
 * Pure helpers behind the monthly timetable screen, kept out of the Composable so the date maths
 * and the "incomplete day" rule can be unit-tested.
 */
object PrayerMonth {

    /** Days in the given Gregorian month, newest-last. Handles leap years through the JDK calendar. */
    fun datesIn(year: Int, month: Int): List<LocalDate> {
        val first = LocalDate.of(year, month, 1)
        return (1..first.lengthOfMonth()).map { first.withDayOfMonth(it) }
    }

    /** Year/month pair `offset` months away from the given month, wrapping across year ends. */
    fun shift(year: Int, month: Int, offset: Int): Pair<Int, Int> {
        val base = LocalDate.of(year, month, 1).plusMonths(offset.toLong())
        return base.year to base.monthValue
    }

    /**
     * Builds display rows for [days]. Days whose obligatory times are missing keep their row (so the
     * month always lists every date) but are flagged [TimetableRow.complete] = false, which the UI
     * renders as a visible gap instead of "00:00" placeholders.
     */
    fun build(
        days: List<DayPrayerTimes>,
        zone: ZoneId,
        hijriOffset: Int,
        formatter: DateTimeFormatter,
    ): List<TimetableRow> = days.sortedBy { it.date }.map { day ->
        val hijri = HijriCalendar.fromGregorian(day.date, hijriOffset)
        TimetableRow(
            date = day.date,
            hijriDay = hijri.day,
            hijriMonth = hijri.month,
            fajr = day.time(PrayerKey.FAJR)?.atZone(zone)?.format(formatter),
            sunrise = day.time(PrayerKey.SUNRISE)?.atZone(zone)?.format(formatter),
            dhuhr = day.time(PrayerKey.DHUHR)?.atZone(zone)?.format(formatter),
            asr = day.time(PrayerKey.ASR)?.atZone(zone)?.format(formatter),
            maghrib = day.time(PrayerKey.MAGHRIB)?.atZone(zone)?.format(formatter),
            isha = day.time(PrayerKey.ISHA)?.atZone(zone)?.format(formatter),
            complete = day.hasAllObligatory,
        )
    }

    /** Dates in [month] that the schedule could not fill — used for an honest partial-data warning. */
    fun missingDates(days: List<TimetableRow>, year: Int, month: Int): List<LocalDate> {
        val present = days.filter { it.complete }.map { it.date }.toSet()
        return datesIn(year, month).filterNot { present.contains(it) }
    }
}
