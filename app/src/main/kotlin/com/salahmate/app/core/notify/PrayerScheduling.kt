package com.salahmate.app.core.notify

import kotlinx.coroutines.flow.first
import android.content.Context
import com.salahmate.app.AppContainer
import com.salahmate.app.core.prefs.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Glue between settings, the prayer repository and the alarm scheduler.
 *
 * Every settings change, boot, timezone change and app start funnels through here so the scheduled
 * window is always derived from the *current* location, method and adjustments.
 */
object PrayerScheduling {

    suspend fun ensureScheduled(
        context: Context,
        container: AppContainer,
        settings: UserSettings,
        horizonDays: Int = 7,
    ): ScheduleReport = withContext(Dispatchers.IO) {
        val scheduler = PrayerAlarmScheduler(context)
        if (!settings.notificationsEnabled) {
            scheduler.cancelAll()
            return@withContext ScheduleReport(0, 0, exact = scheduler.canScheduleExact(), granted = true)
        }
        if (!NotificationPermission.isGranted(context)) {
            // Do not claim reminders are active when the OS will drop them.
            return@withContext ScheduleReport(0, 0, exact = scheduler.canScheduleExact(), granted = false)
        }
        val zone = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val today = LocalDate.now(zone)
        val days = (0 until horizonDays).map { offset ->
            container.prayerTimes.loadDay(settings, today.plusDays(offset.toLong()), allowNetwork = false).day
        }.filter { it.hasAllObligatory }

        val alarms = scheduler.computeAlarms(days, settings, Instant.now(), horizonDays)
        scheduler.scheduleAll(alarms)
    }

    suspend fun rescheduleAfterReboot(context: Context, container: AppContainer) {
        val settings = container.settings.settings.first()
        ensureScheduled(context, container, settings)
        // Also kick the widget so it reflects the new state after a reboot.
        com.salahmate.app.widget.PrayerWidgetUpdater.updateNow(context, container)
    }
}
