package com.salahmate.app.data

import com.salahmate.app.core.prefs.CalculationMethodId
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.net.AppJson
import com.salahmate.app.data.prayer.AdhanTimingsResponse
import com.salahmate.app.data.prayer.AlAdhanProvider
import com.salahmate.app.data.prayer.PrayerRequest
import com.salahmate.app.domain.prayer.PrayerKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Parser tests driven by response fixtures captured from the live AlAdhan API on 2026-10-24.
 * The network is never used here: fixtures make the suite deterministic.
 */
class AlAdhanParsingTest {

    private val request = PrayerRequest(
        date = LocalDate.of(2026, 10, 24),
        latitude = 23.8103,
        longitude = 90.4125,
        zoneId = "Asia/Dhaka",
        city = "Dhaka",
        country = "Bangladesh",
        settings = UserSettings(method = CalculationMethodId.KARACHI),
    )

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "fixture $name missing"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `parses recorded dhaka response into instants`() {
        val response = AppJson.decodeFromString<AdhanTimingsResponse>(fixture("aladhan_dhaka_2026-10-24.json"))
        assertEquals(200, response.code)
        val day = AlAdhanProvider.parseDay(response.data!!, request)
        assertNotNull(day)
        val zone = ZoneId.of("Asia/Dhaka")
        fun local(k: PrayerKey) = day!!.time(k)!!.atZone(zone).toLocalTime()
        assertEquals(4, local(PrayerKey.FAJR).hour)
        assertEquals(44, local(PrayerKey.FAJR).minute)
        assertEquals(18, local(PrayerKey.ISHA).hour)
        assertEquals(41, local(PrayerKey.ISHA).minute)
        assertTrue(day!!.hasAllObligatory)
        // Meta timezone in the fixture is Asia/Dhaka and must be preferred over the request zone.
        assertEquals(zone, day!!.zoneId)
    }

    @Test
    fun `parses london response using its own timezone`() {
        val response = AppJson.decodeFromString<AdhanTimingsResponse>(fixture("aladhan_london_2026-10-24.json"))
        val londonRequest = request.copy(zoneId = "Europe/London", city = "London", country = "United Kingdom")
        val day = AlAdhanProvider.parseDay(response.data!!, londonRequest)
        assertNotNull(day)
        assertEquals(ZoneId.of("Europe/London"), day!!.zoneId)
        val dayNonNull = day!!
        val maghrib = dayNonNull.time(PrayerKey.MAGHRIB)!!.atZone(dayNonNull.zoneId).toLocalTime()
        assertEquals(17, maghrib.hour)
        assertEquals(49, maghrib.minute)
    }

    @Test
    fun `strips utc offset suffixes used by calendar responses`() {
        val instant = AlAdhanProvider.parseClockToInstant("04:35 (+06)", LocalDate.of(2026, 10, 1), ZoneId.of("Asia/Dhaka"))
        assertNotNull(instant)
        val local = instant!!.atZone(ZoneId.of("Asia/Dhaka")).toLocalTime()
        assertEquals(4, local.hour)
        assertEquals(35, local.minute)
    }

    @Test
    fun `rejects malformed clock values instead of inventing times`() {
        assertNull(AlAdhanProvider.parseClockToInstant("not a time", LocalDate.of(2026, 10, 1), ZoneId.of("Asia/Dhaka")))
        assertNull(AlAdhanProvider.parseClockToInstant("04:99", LocalDate.of(2026, 10, 1), ZoneId.of("Asia/Dhaka")))
        assertNull(AlAdhanProvider.parseClockToInstant("", LocalDate.of(2026, 10, 1), ZoneId.of("Asia/Dhaka")))
    }

    @Test
    fun `missing obligatory prayer makes the day unusable rather than partial`() {
        val response = AppJson.decodeFromString<AdhanTimingsResponse>(fixture("aladhan_dhaka_2026-10-24.json"))
        val data = response.data!!.copy(timings = response.data.timings - "Isha")
        assertNull(AlAdhanProvider.parseDay(data, request))
    }

    @Test
    fun `calendar fixture exposes 31 days with offset suffixes`() {
        val calendar = AppJson.decodeFromString<com.salahmate.app.data.prayer.AdhanCalendarResponse>(
            fixture("aladhan_dhaka_calendar_2026-10.json"),
        )
        assertEquals(31, calendar.data.size)
        val first = calendar.data.first()
        val day = AlAdhanProvider.parseDay(first, request.copy(date = LocalDate.of(2026, 10, 1)))
        assertNotNull(day)
        assertTrue(day!!.hasAllObligatory)
    }
}
