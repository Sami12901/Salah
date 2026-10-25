package com.salahmate.app.core.notify

import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.domain.prayer.PrayerKey

/** Why a reminder fired. Drives the wording so "in 10 minutes" is never shown for an on-time alert. */
enum class NotificationKind { ADVANCE, AT_TIME, SNOOZED }

enum class NotificationUrgency { NORMAL, HIGH }

/**
 * Wording and channel selection for prayer reminders, kept pure so it can be tested without posting
 * anything. [PrayerNotifier] owns the Android side.
 */
object NotificationContent {

    fun kindFor(advanceMinutes: Int, snoozed: Boolean): NotificationKind = when {
        snoozed -> NotificationKind.SNOOZED
        advanceMinutes > 0 -> NotificationKind.ADVANCE
        else -> NotificationKind.AT_TIME
    }

    fun title(prayer: PrayerKey, advanceMinutes: Int, snoozed: Boolean, bangla: Boolean): String {
        val name = if (bangla) prayer.labelBn else prayer.labelEn
        return when (kindFor(advanceMinutes, snoozed)) {
            NotificationKind.ADVANCE ->
                if (bangla) "$name আর $advanceMinutes মিনিট পরে" else "$name in $advanceMinutes minutes"
            NotificationKind.SNOOZED ->
                if (bangla) "$name — রিমাইন্ডার" else "$name — reminder"
            NotificationKind.AT_TIME ->
                if (bangla) "$name-এর ওয়াক্ত হয়েছে" else "$name time"
        }
    }

    /** Silent mode must not use the audible channel, otherwise the OS would play a sound anyway. */
    fun channelFor(soundMode: NotificationSoundMode): String = when (soundMode) {
        NotificationSoundMode.SILENT -> NotificationChannels.PRAYER_SILENT
        NotificationSoundMode.SYSTEM_SOUND, NotificationSoundMode.ADHAN_AUDIO -> NotificationChannels.PRAYER
    }

    fun urgencyFor(soundMode: NotificationSoundMode): NotificationUrgency =
        if (soundMode == NotificationSoundMode.SILENT) NotificationUrgency.NORMAL else NotificationUrgency.HIGH

    /** Stable notification id per prayer so a new alert replaces the previous one instead of stacking. */
    fun idFor(prayer: PrayerKey): Int = 2000 + prayer.ordinal

    const val TEST_NOTIFICATION_ID = 9001
}
