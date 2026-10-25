package com.salahmate.app.workers

import kotlinx.coroutines.flow.first
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salahmate.app.SalahMateApplication
import com.salahmate.app.core.notify.PrayerScheduling
import com.salahmate.app.widget.PrayerWidgetUpdater
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Once-a-day safety net: re-arms the alarm window, prunes the prayer cache and refreshes the widget.
 *
 * This is the *only* periodic background work in the app; it is deliberately deferrable so the OS
 * can batch it, and nothing user-facing depends on it running at an exact time (the alarms do the
 * time-critical work).
 */
class MaintenanceWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SalahMateApplication ?: return Result.success()
        val container = app.container
        return runCatching {
            val settings = container.settings.settings.first()
            PrayerScheduling.ensureScheduled(applicationContext, container, settings)
            container.prayerTimes.pruneCache(Instant.now().minus(60, ChronoUnit.DAYS))
            PrayerWidgetUpdater.updateNow(applicationContext, container)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val NAME = "salahmate_maintenance"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            runCatching {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }
        }
    }
}
