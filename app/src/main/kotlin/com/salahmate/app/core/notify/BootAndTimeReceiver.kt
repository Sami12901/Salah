package com.salahmate.app.core.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.salahmate.app.SalahMateApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms reminders after a reboot, a timezone change, a manual clock change or a date rollover.
 * Android clears alarms on reboot, so this receiver is what keeps reminders working afterwards.
 */
class BootAndTimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return
        val app = context.applicationContext as? SalahMateApplication ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerScheduling.rescheduleAfterReboot(context, app.container)
            } catch (t: Throwable) {
                // A failure here must not crash the app on boot; the daily worker retries.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_DATE_CHANGED,
        )
    }
}
