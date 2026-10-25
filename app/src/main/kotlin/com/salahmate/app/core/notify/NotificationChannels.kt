package com.salahmate.app.core.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build

/** Notification channel definitions. Channels are created once and reused; the OS owns the rest. */
object NotificationChannels {

    const val PRAYER = "prayer_times"
    const val PRAYER_SILENT = "prayer_times_silent"
    const val RAMADAN = "ramadan"
    const val GENERIC = "general"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        manager.createNotificationChannel(
            NotificationChannel(PRAYER, "Prayer reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Adhan and prayer time reminders"
                enableVibration(true)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )

        manager.createNotificationChannel(
            // IMPORTANCE_LOW is the highest level that never makes a sound. IMPORTANCE_DEFAULT would
            // still play the notification tone, which would defeat the whole point of "silent".
            NotificationChannel(PRAYER_SILENT, "Silent prayer reminders", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Silent prayer reminders (notification without sound)"
                setSound(null, null)
                enableVibration(false)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(RAMADAN, "Ramadan reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Suhoor and Iftar reminders during Ramadan"
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build(),
                )
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(GENERIC, "General", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Downloads, backups and other app messages"
            },
        )
    }
}
