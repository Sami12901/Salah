package com.salahmate.app.core

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.salahmate.app.core.notify.NotificationChannels
import com.salahmate.app.core.notify.PrayerNotifier
import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.domain.prayer.PrayerKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Notification *posting* on a Robolectric-managed Android runtime: channels are created with the
 * right importance, the reminder lands on the expected channel with the expected wording, and a
 * snooze action is attached.
 *
 * What this still cannot prove: that the OS actually delivers the alarm at the scheduled minute,
 * and how it looks on a real device. That needs the manual checklist in TESTING.md.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationPostingTest {

    private lateinit var context: Context
    private lateinit var manager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Robolectric starts with POST_NOTIFICATIONS denied (as on a fresh install), so grant it to
        // test the posting path. The denial path is covered by `nothing is posted without permission`.
        shadowOf(context as android.app.Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `channels are created with prayer and silent importance`() {
        NotificationChannels.ensureCreated(context)

        val prayer = manager.getNotificationChannel(NotificationChannels.PRAYER)
        val silent = manager.getNotificationChannel(NotificationChannels.PRAYER_SILENT)

        assertNotNull("prayer channel missing", prayer)
        assertNotNull("silent channel missing", silent)
        // The audible channel must be able to make a sound; the silent one must not.
        assertTrue(prayer.importance >= NotificationManager.IMPORTANCE_DEFAULT)
        assertEquals(NotificationManager.IMPORTANCE_LOW, silent.importance)
        assertEquals(null, silent.sound)
    }

    @Test
    fun `a prayer reminder is posted on the audible channel with a snooze action`() {
        NotificationChannels.ensureCreated(context)

        PrayerNotifier.showPrayerReminder(
            context = context,
            prayer = PrayerKey.FAJR,
            advanceMinutes = 10,
            soundMode = NotificationSoundMode.SYSTEM_SOUND,
            vibrate = true,
        )

        val posted = shadowOf(manager).allNotifications
        assertEquals(1, posted.size)
        val notification = posted.first()
        assertEquals(NotificationChannels.PRAYER, notification.channelId)

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        assertEquals("Fajr in 10 minutes", title)
        // Snooze must be offered as an action so the user can defer without opening the app.
        assertEquals(1, notification.actions.size)
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun `silent setting posts on the silent channel`() {
        NotificationChannels.ensureCreated(context)

        PrayerNotifier.showPrayerReminder(
            context = context,
            prayer = PrayerKey.ISHA,
            advanceMinutes = 0,
            soundMode = NotificationSoundMode.SILENT,
            vibrate = false,
            snoozed = true,
            bangla = true,
        )

        val notification = shadowOf(manager).allNotifications.first()
        assertEquals(NotificationChannels.PRAYER_SILENT, notification.channelId)
        assertEquals(
            "এশা — রিমাইন্ডার",
            notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        )
    }

    @Test
    fun `a newer reminder for the same prayer replaces the old one instead of stacking`() {
        NotificationChannels.ensureCreated(context)

        PrayerNotifier.showPrayerReminder(context, PrayerKey.DHUHR, 0, NotificationSoundMode.SYSTEM_SOUND, true)
        PrayerNotifier.showPrayerReminder(context, PrayerKey.DHUHR, 0, NotificationSoundMode.SYSTEM_SOUND, true)

        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `nothing is posted when the user has not granted notifications`() {
        NotificationChannels.ensureCreated(context)
        shadowOf(context as android.app.Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        PrayerNotifier.showPrayerReminder(
            context = context,
            prayer = PrayerKey.FAJR,
            advanceMinutes = 0,
            soundMode = NotificationSoundMode.SYSTEM_SOUND,
            vibrate = true,
        )

        // Silently doing nothing is the correct behaviour: the UI reports the permission state.
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `test notification is clearly labelled and cancellable`() {
        NotificationChannels.ensureCreated(context)

        PrayerNotifier.showTestNotification(context, NotificationSoundMode.SYSTEM_SOUND)
        val notification = shadowOf(manager).allNotifications.first()
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        assertTrue("test notification title was '$title'", title.isNotEmpty())

        PrayerNotifier.cancelAll(context)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
}
