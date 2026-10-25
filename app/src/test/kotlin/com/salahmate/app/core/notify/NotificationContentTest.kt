package com.salahmate.app.core.notify

import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.domain.prayer.PrayerKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Reminder wording, channel selection and id stability. */
class NotificationContentTest {

    @Test
    fun `kind distinguishes advance, on-time and snoozed`() {
        assertEquals(NotificationKind.ADVANCE, NotificationContent.kindFor(10, snoozed = false))
        assertEquals(NotificationKind.AT_TIME, NotificationContent.kindFor(0, snoozed = false))
        // A snoozed alert is a reminder even when the original had an advance offset.
        assertEquals(NotificationKind.SNOOZED, NotificationContent.kindFor(10, snoozed = true))
        assertEquals(NotificationKind.SNOOZED, NotificationContent.kindFor(0, snoozed = true))
    }

    @Test
    fun `titles state the advance amount only when there is one`() {
        assertEquals(
            "Fajr in 10 minutes",
            NotificationContent.title(PrayerKey.FAJR, 10, snoozed = false, bangla = false),
        )
        assertEquals(
            "Fajr time",
            NotificationContent.title(PrayerKey.FAJR, 0, snoozed = false, bangla = false),
        )
        assertEquals(
            "Isha — reminder",
            NotificationContent.title(PrayerKey.ISHA, 0, snoozed = true, bangla = false),
        )
        assertEquals(
            "মাগরিব আর 15 মিনিট পরে",
            NotificationContent.title(PrayerKey.MAGHRIB, 15, snoozed = false, bangla = true),
        )
        assertEquals(
            "মাগরিব-এর ওয়াক্ত হয়েছে",
            NotificationContent.title(PrayerKey.MAGHRIB, 0, snoozed = false, bangla = true),
        )
    }

    @Test
    fun `silent mode uses the silent channel so the OS cannot play sound`() {
        assertEquals(NotificationChannels.PRAYER_SILENT, NotificationContent.channelFor(NotificationSoundMode.SILENT))
        assertEquals(NotificationChannels.PRAYER, NotificationContent.channelFor(NotificationSoundMode.SYSTEM_SOUND))
        assertEquals(NotificationChannels.PRAYER, NotificationContent.channelFor(NotificationSoundMode.ADHAN_AUDIO))
        assertEquals(
            NotificationUrgency.NORMAL,
            NotificationContent.urgencyFor(NotificationSoundMode.SILENT),
        )
        assertEquals(
            NotificationUrgency.HIGH,
            NotificationContent.urgencyFor(NotificationSoundMode.SYSTEM_SOUND),
        )
    }

    @Test
    fun `notification ids are stable and unique per prayer`() {
        val ids = PrayerKey.obligatory.map { NotificationContent.idFor(it) }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(NotificationContent.idFor(PrayerKey.FAJR), NotificationContent.idFor(PrayerKey.FAJR))
        assertNotEquals(
            NotificationContent.idFor(PrayerKey.FAJR),
            NotificationContent.idFor(PrayerKey.ISHA),
        )
        assertEquals(9001, NotificationContent.TEST_NOTIFICATION_ID)
    }
}
