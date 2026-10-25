package com.salahmate.app.core.notify

import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The alarm *computation* (which reminders exist, at what instant) is pure and verified here.
 * Actually firing at that instant is an OS behaviour and still needs a device.
 */
class PrayerAlarmComputeTest {

    private val zone: ZoneId = ZoneId.of("Asia/Dhaka")
    private val date: LocalDate = LocalDate.of(2026, 10, 24)

    private fun day(date: LocalDate = this.date): DayPrayerTimes {
        val base = date.atStartOfDay(zone).toInstant()
        return DayPrayerTimes(
            date = date,
            zoneId = zone,
            times = mapOf(
                PrayerKey.FAJR to base.plusSeconds(4 * 3600L + 44 * 60),
                PrayerKey.SUNRISE to base.plusSeconds(6 * 3600L),
                PrayerKey.DHUHR to base.plusSeconds(11 * 3600L + 43 * 60),
                PrayerKey.ASR to base.plusSeconds(15 * 3600L + 49 * 60),
                PrayerKey.MAGHRIB to base.plusSeconds(17 * 3600L + 25 * 60),
                PrayerKey.ISHA to base.plusSeconds(18 * 3600L + 41 * 60),
            ),
            source = PrayerSource.LOCAL_ADHAN,
        )
    }

    private fun settings(
        notifications: Boolean = true,
        advance: Int = 0,
        fajr: Boolean = true,
    ) = UserSettings(
        notificationsEnabled = notifications,
        advanceMinutes = advance,
        soundMode = NotificationSoundMode.SYSTEM_SOUND,
        notifyFajr = fajr,
        notifyDhuhr = true,
        notifyAsr = true,
        notifyMaghrib = true,
        notifyIsha = true,
    )

    /** Uses the Android-free part of the scheduler by constructing it without a Context. */
    private fun compute(
        days: List<DayPrayerTimes>,
        settings: UserSettings,
        now: Instant,
    ): List<PrayerAlarm> = AlarmMath.compute(days, settings, now, horizonDays = 7)

    @Test
    fun `nothing is scheduled when reminders are disabled`() {
        val now = Instant.parse("2026-10-23T20:00:00Z")
        assertTrue(compute(listOf(day()), settings(notifications = false), now).isEmpty())
    }

    @Test
    fun `five obligatory prayers produce five reminders and sunrise never does`() {
        val now = Instant.parse("2026-10-23T20:00:00Z") // 02:00 Dhaka, before Fajr
        val alarms = compute(listOf(day()), settings(), now)
        assertEquals(5, alarms.size)
        assertTrue(alarms.none { it.prayer == PrayerKey.SUNRISE })
        assertEquals(
            listOf(PrayerKey.FAJR, PrayerKey.DHUHR, PrayerKey.ASR, PrayerKey.MAGHRIB, PrayerKey.ISHA),
            alarms.map { it.prayer },
        )
    }

    @Test
    fun `advance minutes shift the trigger and per-prayer opt-out is honoured`() {
        val now = Instant.parse("2026-10-23T20:00:00Z")
        val alarms = compute(listOf(day()), settings(advance = 15, fajr = false), now)

        assertEquals(4, alarms.size)
        assertTrue(alarms.none { it.prayer == PrayerKey.FAJR })
        val dhuhr = alarms.first { it.prayer == PrayerKey.DHUHR }
        assertEquals(15, dhuhr.advanceMinutes)
        // 11:43 minus 15 minutes = 11:28 Dhaka.
        assertEquals(Instant.parse("2026-10-24T05:28:00Z"), dhuhr.triggerAt)
    }

    @Test
    fun `alarms already in the past are dropped`() {
        // 18:00 Dhaka: Fajr/Dhuhr/Asr are past and so is Maghrib (17:25). Only Isha remains.
        val now = Instant.parse("2026-10-24T12:00:00Z")
        val alarms = compute(listOf(day()), settings(), now)
        assertEquals(listOf(PrayerKey.ISHA), alarms.map { it.prayer })
        assertTrue(alarms.all { it.triggerAt.isAfter(now) })
    }

    @Test
    fun `results are sorted and respect the day horizon`() {
        val days = (0..9).map { day(date.plusDays(it.toLong())) }
        val now = Instant.parse("2026-10-23T20:00:00Z")
        val alarms = compute(days, settings(), now)

        assertEquals(alarms.sortedBy { it.triggerAt }, alarms)
        // 7 days x 5 prayers, plus the remainder of the first day's five.
        assertEquals(35, alarms.size)
        assertEquals(date, alarms.first().date)
        assertEquals(date.plusDays(6), alarms.last().date)
    }
}
