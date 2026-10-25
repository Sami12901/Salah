package com.salahmate.app.domain.prayer

import com.salahmate.app.core.util.HijriCalendar
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * One Ramadan day. Imsak, Fajr, Maghrib and Iftar are kept as *distinct* values with their own
 * labels: Imsak is a precautionary stop before Fajr, Iftar is the meal at Maghrib, and the app
 * never presents them as interchangeable.
 */
data class RamadanDay(
    val hijriDay: Int,
    val gregorianDate: LocalDate,
    val imsak: Instant,
    val fajr: Instant,
    val maghrib: Instant,
    val iftar: Instant,
    val zoneId: java.time.ZoneId,
) {
    val fastingLength: Duration get() = Duration.between(imsak, iftar)
}

object RamadanCalendar {

    fun ramadanMonth(hijriYear: Int, offsetDays: Int): Pair<LocalDate, LocalDate> {
        val start = HijriCalendar.gregorianFor(hijriYear, 9, 1).plusDays(offsetDays.toLong())
        val end = HijriCalendar.gregorianFor(hijriYear, 9, 30).plusDays(offsetDays.toLong())
        return start to end
    }

    suspend fun buildDays(
        hijriYear: Int,
        offsetDays: Int,
        imsakOffsetMinutes: Int,
        iftarOffsetMinutes: Int,
        scheduleFor: suspend (LocalDate) -> DayPrayerTimes?,
    ): List<RamadanDay> {
        val (start, end) = ramadanMonth(hijriYear, offsetDays)
        val days = mutableListOf<RamadanDay>()
        var date = start
        var index = 1
        while (!date.isAfter(end) && index <= 30) {
            val schedule = scheduleFor(date)
            val fajr = schedule?.time(PrayerKey.FAJR)
            val maghrib = schedule?.time(PrayerKey.MAGHRIB)
            if (fajr != null && maghrib != null) {
                val imsak = schedule.imsak ?: fajr.minusSeconds(imsakOffsetMinutes * 60L)
                days.add(
                    RamadanDay(
                        hijriDay = index,
                        gregorianDate = date,
                        imsak = imsak,
                        fajr = fajr,
                        maghrib = maghrib,
                        iftar = maghrib.plusSeconds(iftarOffsetMinutes * 60L),
                        zoneId = schedule.zoneId,
                    ),
                )
            }
            date = date.plusDays(1)
            index++
        }
        return days
    }

    /** The currently running fast, if any, with a countdown target for the next event. */
    fun currentState(days: List<RamadanDay>, now: Instant): RamadanState? {
        if (days.isEmpty()) return null
        val active = days.firstOrNull { now.isBefore(it.iftar) && !now.isBefore(it.imsak) }
        if (active != null) {
            return RamadanState(
                day = active,
                phase = RamadanPhase.FASTING,
                nextEvent = "Iftar",
                nextEventTime = active.iftar,
                remaining = Duration.between(now, active.iftar),
            )
        }
        val upcoming = days.firstOrNull { it.imsak.isAfter(now) }
        if (upcoming != null) {
            return RamadanState(
                day = upcoming,
                phase = RamadanPhase.BEFORE_SUHOOR,
                nextEvent = "Suhoor ends (Imsak)",
                nextEventTime = upcoming.imsak,
                remaining = Duration.between(now, upcoming.imsak),
            )
        }
        return null
    }
}

enum class RamadanPhase { BEFORE_SUHOOR, FASTING, AFTER_IFTAR }

data class RamadanState(
    val day: RamadanDay,
    val phase: RamadanPhase,
    val nextEvent: String,
    val nextEventTime: Instant,
    val remaining: Duration,
)
