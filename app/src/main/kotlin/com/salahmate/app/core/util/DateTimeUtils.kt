package com.salahmate.app.core.util

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * All time-dependent code takes a [Clock] so tests can pin "now" instead of relying on the
 * wall clock. Production code resolves [SystemClockProvider.clock].
 */
interface ClockProvider { val clock: Clock }

object SystemClockProvider : ClockProvider {
    override val clock: Clock = Clock.systemUTC()
}

fun Instant.toLocalTime(zone: ZoneId): LocalTime = atZone(zone).toLocalTime()
fun Instant.toLocalDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

/** Human readable duration like "2h 14m" / "45s". Negative durations clamp to zero. */
fun formatCountdown(duration: Duration, includeSecondsWhenShort: Boolean = true): String {
    val total = duration.seconds.coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> if (includeSecondsWhenShort && minutes < 10) "${minutes}m ${seconds}s" else "${minutes}m"
        else -> "${seconds}s"
    }
}

fun formatTime(instant: Instant, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern("h:mm a", locale).withZone(zone).format(instant)

fun formatTime24(instant: Instant, zone: ZoneId): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(instant)

fun formatDateFull(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))

fun formatDateMedium(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

fun formatMonthYear(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))

/** Minutes between two instants, rounded, used for adjustment summaries. */
fun minutesBetween(from: Instant, to: Instant): Long =
    Duration.between(from, to).toMinutes()
