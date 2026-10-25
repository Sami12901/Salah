package com.salahmate.app.core.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salahmate.app.MainActivity
import com.salahmate.app.R
import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.domain.prayer.PrayerKey

/**
 * Builds and posts prayer notifications.
 *
 * Sound behaviour is explicit: silent reminders use a silent channel, system sound uses the
 * notification channel's default sound, and "adhan audio" currently posts a high-importance
 * notification whose channel carries the default alarm sound — the app does not bundle an Adhan
 * recording because no recording with verified redistribution rights is included in this build.
 * That limitation is documented in CONTENT_LICENSES.md instead of being papered over.
 */
object PrayerNotifier {

    private var nextId = 1000

    fun showPrayerReminder(
        context: Context,
        prayer: PrayerKey,
        advanceMinutes: Int,
        soundMode: NotificationSoundMode,
        vibrate: Boolean,
        snoozed: Boolean = false,
        bangla: Boolean = false,
    ) {
        if (!NotificationPermission.isGranted(context)) return

        val channel = NotificationContent.channelFor(soundMode)
        val title = NotificationContent.title(prayer, advanceMinutes, snoozed, bangla)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            prayer.id.hashCode(),
            Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.id)
                putExtra(PrayerAlarmReceiver.EXTRA_SNOOZED, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_prayer_body))
            .setPriority(
                if (NotificationContent.urgencyFor(soundMode) == NotificationUrgency.HIGH) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.notification_snooze), snoozeIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        if (vibrate && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        }
        if (soundMode == NotificationSoundMode.SILENT && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(null)
        }

        try {
            NotificationManagerCompat.from(context).notify(NotificationContent.idFor(prayer), builder.build())
        } catch (_: SecurityException) {
            // Permission may be revoked by the OS between the explicit grant check and posting.
        }
    }

    /** Test notification requested from Settings — clearly labelled as a test. */
    fun showTestNotification(context: Context, soundMode: NotificationSoundMode) {
        if (!NotificationPermission.isGranted(context)) return
        val channel = NotificationContent.channelFor(soundMode)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setContentText(context.getString(R.string.notification_test_body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        try {
            NotificationManagerCompat.from(context).notify(NotificationContent.TEST_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission may be revoked by the OS between the explicit grant check and posting.
        }
    }

    /**
     * Cancels every notification this app can own: one id per prayer plus the test reminder.
     * `NotificationManager.cancelAll()` would also clear channels belonging to other in-app
     * features, so the ids are cancelled explicitly.
     */
    fun cancelAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        PrayerKey.obligatory.forEach { manager.cancel(NotificationContent.idFor(it)) }
        manager.cancel(NotificationContent.TEST_NOTIFICATION_ID)
    }
}
