package com.salahmate.app.core.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.salahmate.app.SalahMateApplication
import com.salahmate.app.domain.prayer.PrayerKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives a prayer alarm and posts the reminder. Also (re)arms the following days' alarms so a
 * single scheduled window keeps itself alive without a polling service.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerId = intent.getStringExtra(EXTRA_PRAYER) ?: return
        val prayer = PrayerKey.from(prayerId) ?: return
        val advance = intent.getIntExtra(EXTRA_ADVANCE, 0)
        val snoozed = intent.getBooleanExtra(EXTRA_SNOOZED, false)

        val pendingResult = goAsync()
        val app = context.applicationContext as? SalahMateApplication ?: run {
            pendingResult.finish(); return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_000) {
                    val settings = com.salahmate.app.core.prefs.UserSettings()
                    val current = runCatching {
                        app.container.settings.settings.first()
                    }.getOrDefault(settings)

                    if (current.notificationsEnabled || snoozed) {
                        PrayerNotifier.showPrayerReminder(
                            context = context,
                            prayer = prayer,
                            advanceMinutes = advance,
                            soundMode = current.soundMode,
                            vibrate = current.vibrate,
                            snoozed = snoozed,
                            bangla = current.isBanglaUi,
                        )
                    }
                    // Re-arm forward so the chain continues even after reboots of the process.
                    PrayerScheduling.ensureScheduled(context, app.container, current)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.salahmate.app.PRAYER_ALARM"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_ADVANCE = "advance"
        const val EXTRA_DATE = "date"
        const val EXTRA_SNOOZED = "snoozed"
    }
}
