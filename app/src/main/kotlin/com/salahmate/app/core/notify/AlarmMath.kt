package com.salahmate.app.core.notify

import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * One reminder to schedule.
 *
 * Kept free of Android types so the schedule can be verified in plain JVM tests; the AlarmManager
 * plumbing lives in [PrayerAlarmScheduler].
 */
data class PrayerAlarm(
    val prayer: PrayerKey,
    val triggerAt: Instant,
    val date: LocalDate,
    val advanceMinutes: Int,
)

/**
 * Pure reminder planning.
 *
 * Rules that matter and are covered by tests:
 *  • nothing at all when reminders are switched off;
 *  • sunrise is never a reminder (it is not a prayer);
 *  • a prayer switched off individually produces no reminder;
 *  • advance reminders shift the trigger, and a trigger already in the past is dropped rather than
 *    fired immediately;
 *  • the result is sorted, so the caller can schedule in order.
 */
object AlarmMath {

    fun compute(
        days: List<DayPrayerTimes>,
        settings: UserSettings,
        now: Instant,
        horizonDays: Int = 7,
    ): List<PrayerAlarm> {
        if (!settings.notificationsEnabled) return emptyList()
        val advance = settings.advanceMinutes.coerceAtLeast(0)
        val alarms = mutableListOf<PrayerAlarm>()
        days.take(horizonDays).forEach { day ->
            PrayerKey.obligatory.forEach { key ->
                if (!settings.isPrayerNotificationEnabled(key.id)) return@forEach
                val time = day.time(key) ?: return@forEach
                val trigger = time.minus(Duration.ofMinutes(advance.toLong()))
                if (trigger.isAfter(now)) {
                    alarms.add(
                        PrayerAlarm(
                            prayer = key,
                            triggerAt = trigger,
                            date = day.date,
                            advanceMinutes = advance,
                        ),
                    )
                }
            }
        }
        return alarms.sortedBy { it.triggerAt }
    }
}
