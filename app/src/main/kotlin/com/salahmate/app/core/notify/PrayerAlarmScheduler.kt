package com.salahmate.app.core.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Platform scheduling. Uses AlarmManager exact alarms when the OS grants that ability, and falls
 * back to inexact alarms (with a user-visible warning) when it does not.
 *
 * No background service and no polling loop: each alarm is a one-shot that re-arms the next window
 * when it fires, plus a daily WorkManager safety net and BOOT/TIME_SET receivers.
 */
class PrayerAlarmScheduler(
    private val context: Context,
) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val prefs = context.applicationContext.getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE)

    fun canScheduleExact(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** Delegates to the pure planner in [AlarmMath]. */
    fun computeAlarms(
        days: List<DayPrayerTimes>,
        settings: UserSettings,
        now: Instant,
        horizonDays: Int = 7,
    ): List<PrayerAlarm> = AlarmMath.compute(days, settings, now, horizonDays)

    /** Schedules [alarms], cancelling any previous ones first so settings changes take effect. */
    fun scheduleAll(alarms: List<PrayerAlarm>): ScheduleReport {
        cancelAll()
        val manager = alarmManager ?: return ScheduleReport(0, 0, exact = false, granted = false)
        val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
        var scheduled = 0
        val registered = mutableSetOf<String>()
        alarms.forEach { alarm ->
            val pending = pendingIntent(alarm)
            val triggerAt = alarm.triggerAt.toEpochMilli()
            val success = try {
                if (exact) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    } else {
                        manager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
                true
            } catch (_: SecurityException) {
                // Exact-alarm access can be revoked after the check. Retry as inexact rather than
                // silently dropping a reminder; report exact=false through the permission check.
                if (exact) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                        } else {
                            manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                        }
                        true
                    } catch (_: SecurityException) {
                        false
                    }
                } else {
                    false
                }
            }
            if (success) {
                scheduled++
                registered += record(alarm)
            }
        }
        if (registered.isEmpty()) {
            prefs.edit().remove(KEY_SCHEDULED).apply()
        } else {
            prefs.edit().putStringSet(KEY_SCHEDULED, registered).apply()
        }
        return ScheduleReport(scheduled, alarms.size, exact = exact, granted = true)
    }

    /**
     * Cancels every alarm this app actually scheduled.
     *
     * PendingIntent identity depends on the prayer, the date and the advance minutes, so the exact
     * records are persisted when scheduling and replayed here — cancelling a guessed date would
     * leave the real alarms alive.
     */
    fun cancelAll() {
        val manager = alarmManager ?: return
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().forEach { record ->
            val parts = record.split('|')
            val key = PrayerKey.from(parts.getOrNull(0)) ?: return@forEach
            val date = parts.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@forEach
            val advance = parts.getOrNull(2)?.toIntOrNull() ?: 0
            runCatching { manager.cancel(prayerPendingIntent(key, date, advance)) }
        }
        prefs.edit().remove(KEY_SCHEDULED).apply()
        // Also cancel any pending snoozes.
        PrayerKey.obligatory.forEach { key ->
            runCatching { manager.cancel(snoozePendingIntent(key)) }
        }
    }

    fun scheduleSnooze(prayer: PrayerKey, minutes: Int = 10) {
        val manager = alarmManager ?: return
        val trigger = System.currentTimeMillis() + minutes * 60_000L
        val pending = snoozePendingIntent(prayer)
        val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (exact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission may be revoked after the check. Keep the snooze inexact.
            if (exact) {
                try {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                } catch (_: SecurityException) {
                    // No notification alarm can be scheduled if the OS rejects even the fallback.
                }
            }
        }
    }

    private fun prayerPendingIntent(key: PrayerKey, date: LocalDate, advance: Int): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, key.id)
            putExtra(PrayerAlarmReceiver.EXTRA_ADVANCE, advance)
            putExtra(PrayerAlarmReceiver.EXTRA_DATE, date.toString())
            // Unique request code per prayer + date + advance keeps pending intents distinct.
            data = android.net.Uri.parse("salahmate://prayer/${key.id}/${date}/$advance")
        }
        return PendingIntent.getBroadcast(
            context,
            (key.id.hashCode() * 31 + date.hashCode() + advance),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(alarm: PrayerAlarm): PendingIntent =
        prayerPendingIntent(alarm.prayer, alarm.date, alarm.advanceMinutes)

    private fun snoozePendingIntent(key: PrayerKey): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, key.id)
            putExtra(PrayerAlarmReceiver.EXTRA_SNOOZED, true)
            data = android.net.Uri.parse("salahmate://prayer/snooze/${key.id}")
        }
        return PendingIntent.getBroadcast(
            context,
            "snooze-${key.id}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private const val ALARM_PREFS = "salahmate_alarms"
private const val KEY_SCHEDULED = "scheduled_alarms"

/** Serialised identity of a scheduled alarm, persisted so it can be cancelled precisely. */
private fun PrayerAlarmScheduler.record(alarm: PrayerAlarm): String =
    "${alarm.prayer.id}|${alarm.date}|${alarm.advanceMinutes}"

data class ScheduleReport(
    val scheduledCount: Int,
    val requestedCount: Int,
    val exact: Boolean,
    val granted: Boolean,
) {
    val allScheduled: Boolean get() = requestedCount == scheduledCount && granted
}
