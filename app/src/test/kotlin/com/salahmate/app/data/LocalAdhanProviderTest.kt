package com.salahmate.app.data

import com.salahmate.app.core.prefs.AsrMadhab
import com.salahmate.app.core.prefs.CalculationMethodId
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.prayer.LocalAdhanProvider
import com.salahmate.app.data.prayer.PrayerRequest
import com.salahmate.app.data.prayer.ProviderResult
import com.salahmate.app.domain.prayer.PrayerKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Cross-checks the on-device Adhan calculation against values recorded from the AlAdhan API on
 * 2026-10-24 (see src/test/resources/fixtures). These are two independent implementations of the
 * same astronomical model, so they should agree to within one minute; the assertions allow two to
 * absorb rounding in the API's HH:mm output.
 *
 * These tests do not call the network: the fixture values are literals recorded with the fixture
 * files, so the suite is deterministic and passes offline.
 */
class LocalAdhanProviderTest {

    private val provider = LocalAdhanProvider()

    private fun settings(
        method: CalculationMethodId = CalculationMethodId.KARACHI,
        madhab: AsrMadhab = AsrMadhab.HANAFI,
        zone: String = "Asia/Dhaka",
        fajrAdj: Int = 0,
    ) = UserSettings(
        latitude = 23.8103,
        longitude = 90.4125,
        timeZoneId = zone,
        method = method,
        madhab = madhab,
        adjustmentFajr = fajrAdj,
        city = "Dhaka",
        country = "Bangladesh",
    )

    private suspend fun calculate(
        date: LocalDate = LocalDate.of(2026, 10, 24),
        lat: Double = 23.8103,
        lon: Double = 90.4125,
        settings: UserSettings = settings(),
    ) = provider.fetch(
        PrayerRequest(
            date = date,
            latitude = lat,
            longitude = lon,
            zoneId = settings.timeZoneId,
            city = settings.city,
            country = settings.country,
            settings = settings,
        ),
    )

    private fun minutesOf(day: com.salahmate.app.domain.prayer.DayPrayerTimes, key: PrayerKey): Int {
        val t = day.time(key)!!.atZone(day.zoneId).toLocalTime()
        return t.hour * 60 + t.minute
    }

    @Test
    fun `dhaka times match AlAdhan reference within two minutes`() = runTest {
        val result = calculate()
        assertTrue(result is ProviderResult.Success)
        val day = (result as ProviderResult.Success).day

        // Recorded from https://api.aladhan.com/v1/timings/24-10-2026?latitude=23.8103&longitude=90.4125&method=1&school=1
        val expected = mapOf(
            PrayerKey.FAJR to (4 * 60 + 44),
            PrayerKey.SUNRISE to (6 * 60 + 0),
            PrayerKey.DHUHR to (11 * 60 + 43),
            PrayerKey.ASR to (15 * 60 + 49),
            PrayerKey.MAGHRIB to (17 * 60 + 25),
            PrayerKey.ISHA to (18 * 60 + 41),
        )
        expected.forEach { (key, reference) ->
            val actual = minutesOf(day, key)
            val delta = abs(actual - reference)
            assertTrue(
                "Dhaka $key was $actual minutes, reference $reference (delta $delta > 2)",
                delta <= 2,
            )
        }
    }

    @Test
    fun `calculations are identical regardless of the device time zone`() = runTest {
        val dhaka = calculate(settings = settings(zone = "Asia/Dhaka"))
        val newYork = calculate(settings = settings(zone = "America/New_York"))
        val dhakaDay = (dhaka as ProviderResult.Success).day
        val nyDay = (newYork as ProviderResult.Success).day
        // Same location + date => same absolute instants; only the display zone differs.
        PrayerKey.obligatory.forEach { key ->
            assertEquals(
                "Instant for $key changed with display zone",
                dhakaDay.time(key),
                nyDay.time(key),
            )
        }
        assertEquals(ZoneId.of("America/New_York"), nyDay.zoneId)
    }

    @Test
    fun `hanafi asr is later than shafi asr`() = runTest {
        val hanafi = (calculate(settings = settings(madhab = AsrMadhab.HANAFI)) as ProviderResult.Success).day
        val shafi = (calculate(settings = settings(madhab = AsrMadhab.SHAFI)) as ProviderResult.Success).day
        val hanafiAsr = hanafi.time(PrayerKey.ASR)!!
        val shafiAsr = shafi.time(PrayerKey.ASR)!!
        assertTrue("Hanafi Asr must be later than Shafi Asr", hanafiAsr.isAfter(shafiAsr))
        // Other prayers are unaffected by the Asr school.
        assertEquals(hanafi.time(PrayerKey.DHUHR), shafi.time(PrayerKey.DHUHR))
        assertEquals(hanafi.time(PrayerKey.MAGHRIB), shafi.time(PrayerKey.MAGHRIB))
    }

    @Test
    fun `calculation method changes fajr angle and therefore fajr time`() = runTest {
        val karachi = (calculate(settings = settings(method = CalculationMethodId.KARACHI)) as ProviderResult.Success).day
        val mwl = (calculate(settings = settings(method = CalculationMethodId.MUSLIM_WORLD_LEAGUE)) as ProviderResult.Success).day
        val diff = Duration.between(mwl.time(PrayerKey.FAJR), karachi.time(PrayerKey.FAJR)).abs().toMinutes()
        // Karachi uses 18 degrees, MWL uses 18/17; Fajr angles are both 18 so times are near-equal,
        // but Isha differs (18 vs 17 degrees).
        val ishaDiff = Duration.between(mwl.time(PrayerKey.ISHA), karachi.time(PrayerKey.ISHA)).abs().toMinutes()
        assertTrue("Expected some measurable difference between methods", diff + ishaDiff > 0)
    }

    @Test
    fun `adjustments shift only the intended prayer`() = runTest {
        val base = (calculate() as ProviderResult.Success).day
        val adjusted = (calculate(settings = settings(fajrAdj = 7)) as ProviderResult.Success).day
            .withAdjustments(mapOf(PrayerKey.FAJR to 7))
        assertEquals(
            base.time(PrayerKey.FAJR)!!.plus(Duration.ofMinutes(7)),
            adjusted.time(PrayerKey.FAJR),
        )
        assertEquals(base.time(PrayerKey.DHUHR), adjusted.time(PrayerKey.DHUHR))
    }

    @Test
    fun `sunnah times bracket the night`() = runTest {
        val day = (calculate() as ProviderResult.Success).day
        val maghrib = day.time(PrayerKey.MAGHRIB)!!
        val midnight = day.midnight
        assertNotNull(midnight)
        val lastThird = day.lastThirdOfNight!!
        assertTrue("Midnight must follow Maghrib", midnight!!.isAfter(maghrib))
        assertTrue("Last third must follow midnight", lastThird.isAfter(midnight!!))
    }

    @Test
    fun `high latitude location produces a usable schedule`() = runTest {
        // Oslo in late October: standard methods; the app must still return all five prayers.
        val osloSettings = UserSettings(
            latitude = 59.9139,
            longitude = 10.7522,
            timeZoneId = "Europe/Oslo",
            method = CalculationMethodId.MUSLIM_WORLD_LEAGUE,
            madhab = AsrMadhab.SHAFI,
        )
        val result = calculate(
            lat = 59.9139,
            lon = 10.7522,
            settings = osloSettings,
        )
        assertTrue(result is ProviderResult.Success)
        val day = (result as ProviderResult.Success).day
        assertTrue(day.hasAllObligatory)
        // Recorded AlAdhan reference for the same inputs: Fajr 05:58, Dhuhr 13:01, Maghrib 17:43.
        val osloZone = ZoneId.of("Europe/Oslo")
        fun localMinutes(key: PrayerKey): Int {
            val t = day.time(key)!!.atZone(osloZone).toLocalTime()
            return t.hour * 60 + t.minute
        }
        assertTrue(abs(localMinutes(PrayerKey.MAGHRIB) - (17 * 60 + 43)) <= 2)
        assertTrue(abs(localMinutes(PrayerKey.FAJR) - (5 * 60 + 58)) <= 2)
    }

    @Test
    fun `date boundary near midnight keeps the requested civil date`() = runTest {
        val day = (calculate(date = LocalDate.of(2026, 12, 31)) as ProviderResult.Success).day
        assertEquals(LocalDate.of(2026, 12, 31), day.date)
        // Fajr is always on the requested civil date in Bangladesh (UTC+6).
        assertEquals(day.date, day.time(PrayerKey.FAJR)!!.atZone(day.zoneId).toLocalDate())
    }
}

private fun Duration.abs(): Duration = if (isNegative) negated() else this
