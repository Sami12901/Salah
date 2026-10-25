package com.salahmate.app.widget

import com.salahmate.app.core.prefs.AppLanguage
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Widget content logic. This covers what the Glance composable renders — next prayer, countdown,
 * schedule rows, Hijri line and the empty state — without needing a launcher.
 *
 * Times are the recorded Dhaka ground truth for 2026-10-24 (Asia/Dhaka, KARACHI + HANAFI).
 */
class WidgetContentTest {

    private val zone: ZoneId = ZoneId.of("Asia/Dhaka")
    private val date: LocalDate = LocalDate.of(2026, 10, 24)

    private fun dhakaDay(complete: Boolean = true): DayPrayerTimes {
        val base = date.atStartOfDay(zone).toInstant()
        val all = mapOf(
            PrayerKey.FAJR to base.plusSeconds(4 * 3600L + 44 * 60 + 30),
            PrayerKey.SUNRISE to base.plusSeconds(6 * 3600L + 30),
            PrayerKey.DHUHR to base.plusSeconds(11 * 3600L + 43 * 60 + 30),
            PrayerKey.ASR to base.plusSeconds(15 * 3600L + 49 * 60 + 30),
            PrayerKey.MAGHRIB to base.plusSeconds(17 * 3600L + 25 * 60 + 30),
            PrayerKey.ISHA to base.plusSeconds(18 * 3600L + 41 * 60 + 30),
        )
        return DayPrayerTimes(
            date = date,
            zoneId = zone,
            times = if (complete) all else all - PrayerKey.FAJR,
            source = PrayerSource.LOCAL_ADHAN,
        )
    }

    private val english = UserSettings(language = AppLanguage.ENGLISH, locationLabel = "Dhaka, Bangladesh")

    @Test
    fun `next prayer and countdown are shown for a time before fajr`() {
        val now = Instant.parse("2026-10-23T20:10:00Z") // 02:10 in Dhaka on the 24th
        val state = WidgetContent.build(english, date, dhakaDay(), null, null, now, zone)

        assertTrue(state.hasData)
        assertEquals("Fajr", state.nextLabel)
        assertEquals("4:44 AM", state.nextTime)
        assertTrue("countdown was '${state.countdown}'", state.countdown.isNotBlank())
        assertEquals("Dhaka, Bangladesh", state.locationLabel)
    }

    @Test
    fun `schedule lists the five obligatory prayers in order`() {
        val now = Instant.parse("2026-10-23T20:10:00Z")
        val state = WidgetContent.build(english, date, dhakaDay(), null, null, now, zone)

        assertEquals(listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"), state.schedule.map { it.first })
        // English uses the 12-hour format from core/util/DateTimeUtils.formatTime.
        assertEquals(
            listOf("4:44 AM", "11:43 AM", "3:49 PM", "5:25 PM", "6:41 PM"),
            state.schedule.map { it.second },
        )
    }

    @Test
    fun `hijri line follows the tabular calendar and the user offset`() {
        val now = Instant.parse("2026-10-23T20:10:00Z")
        val plain = WidgetContent.build(english, date, dhakaDay(), null, null, now, zone)
        assertEquals("12 Jumada al-Ula 1448 AH", plain.hijri)

        val shifted = WidgetContent.build(
            english.copy(hijriOffsetDays = 1), date, dhakaDay(), null, null, now, zone,
        )
        assertEquals("13 Jumada al-Ula 1448 AH", shifted.hijri)
    }

    @Test
    fun `bangla settings produce bangla labels and hijri text`() {
        val bangla = UserSettings(language = AppLanguage.BENGALI, locationLabel = "ঢাকা, বাংলাদেশ")
        val now = Instant.parse("2026-10-23T20:10:00Z")
        val state = WidgetContent.build(bangla, date, dhakaDay(), null, null, now, zone)

        assertEquals("ফজর", state.nextLabel)
        assertEquals(listOf("ফজর", "যোহর", "আসর", "মাগরিব", "এশা"), state.schedule.map { it.first })
        assertTrue(state.hijri.contains("হিজরি"))
        assertEquals("বাংলাদেশ", bangla.locationLabel.substringAfter(", "))
    }

    @Test
    fun `missing schedule yields an explicit empty state instead of fake times`() {
        val now = Instant.parse("2026-10-23T20:10:00Z")

        val noData = WidgetContent.build(english, date, null, null, null, now, zone)
        assertFalse(noData.hasData)
        assertEquals("Dhaka, Bangladesh", noData.locationLabel)
        assertTrue(noData.schedule.isEmpty())
        assertEquals("", noData.nextLabel)

        // A partially calculated day (high latitude) is also treated as no data.
        val partial = WidgetContent.build(english, date, dhakaDay(complete = false), null, null, now, zone)
        assertFalse(partial.hasData)
    }

    @Test
    fun `method label is carried for the expanded layout`() {
        val now = Instant.parse("2026-10-23T20:10:00Z")
        val state = WidgetContent.build(english, date, dhakaDay(), null, null, now, zone)
        assertTrue(state.method.contains("Karachi"))
    }
}
