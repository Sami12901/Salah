package com.salahmate.app.core

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.salahmate.app.core.notify.PrayerAlarm
import com.salahmate.app.core.notify.PrayerAlarmScheduler
import com.salahmate.app.domain.prayer.PrayerKey
import java.time.Instant
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: PrayerAlarmScheduler
    private lateinit var alarmManager: AlarmManager
    private lateinit var preferences: android.content.SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        scheduler = PrayerAlarmScheduler(context)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        preferences = context.getSharedPreferences("salahmate_alarms", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }

    @Test
    fun `scheduled alarms are counted once persisted and cancelled by identity`() {
        val alarm = alarm(PrayerKey.FAJR)

        val report = scheduler.scheduleAll(listOf(alarm))

        assertEquals(1, report.scheduledCount)
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
        assertEquals(
            setOf("FAJR|${alarm.date}|0"),
            preferences.getStringSet("scheduled_alarms", emptySet()),
        )

        scheduler.cancelAll()

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
        assertFalse(preferences.contains("scheduled_alarms"))
    }

    @Test
    fun `missing exact alarm access is reported and reminder falls back to inexact`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        val report = scheduler.scheduleAll(listOf(alarm(PrayerKey.MAGHRIB)))

        assertFalse(report.exact)
        assertEquals(1, report.scheduledCount)
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
    }

    private fun alarm(prayer: PrayerKey) = PrayerAlarm(
        prayer = prayer,
        triggerAt = Instant.now().plusSeconds(3_600),
        date = LocalDate.now(),
        advanceMinutes = 0,
    )
}
