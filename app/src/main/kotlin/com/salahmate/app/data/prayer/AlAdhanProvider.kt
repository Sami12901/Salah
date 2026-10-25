package com.salahmate.app.data.prayer

import com.salahmate.app.core.prefs.AsrMadhab
import com.salahmate.app.core.prefs.CalculationMethodId
import com.salahmate.app.core.prefs.HighLatitudeRuleId
import com.salahmate.app.core.prefs.PrayerProviderMode
import com.salahmate.app.core.net.toProviderError
import com.salahmate.app.core.net.withRetry
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * On-device prayer time calculation using Adhan (com.batoulapps.adhan:adhan:1.2.1, MIT).
 *
 * Verified during implementation (see TESTING.md): the library returns timezone-independent
 * absolute instants, so computing for a chosen location is correct even when the device clock is
 * set to a different zone. Adhan applies neither user adjustments nor AlAdhan-style tuning here —
 * adjustments are applied once, centrally, by PrayerTimesRepository.
 */
class LocalAdhanProvider : PrayerTimesProvider {

    override val id: String = "local_adhan"

    override suspend fun fetch(request: PrayerRequest): ProviderResult = withContext(Dispatchers.Default) {
        try {
            val settings = request.settings
            val parameters = com.batoulapps.adhan.CalculationMethod
                .valueOf(settings.method.adhan)
                .getParameters()

            parameters.madhab = when (settings.madhab) {
                AsrMadhab.HANAFI -> com.batoulapps.adhan.Madhab.HANAFI
                AsrMadhab.SHAFI -> com.batoulapps.adhan.Madhab.SHAFI
            }
            parameters.highLatitudeRule = when (settings.highLatitudeRule) {
                HighLatitudeRuleId.MIDDLE_OF_THE_NIGHT -> com.batoulapps.adhan.HighLatitudeRule.MIDDLE_OF_THE_NIGHT
                HighLatitudeRuleId.SEVENTH_OF_THE_NIGHT -> com.batoulapps.adhan.HighLatitudeRule.SEVENTH_OF_THE_NIGHT
                HighLatitudeRuleId.TWILIGHT_ANGLE -> com.batoulapps.adhan.HighLatitudeRule.TWILIGHT_ANGLE
            }

            val coordinates = com.batoulapps.adhan.Coordinates(request.latitude, request.longitude)
            val components = com.batoulapps.adhan.data.DateComponents(
                request.date.year,
                request.date.monthValue,
                request.date.dayOfMonth,
            )
            val prayerTimes = com.batoulapps.adhan.PrayerTimes(coordinates, components, parameters)
            val zone = ZoneId.of(request.zoneId)

            // Times are truncated to the minute: that is how prayer times are announced and how
            // AlAdhan reports them, and it removes sub-second floating point noise from the
            // astronomical maths so schedules are stable between runs and safe to cache.
            val times = linkedMapOf(
                PrayerKey.FAJR to prayerTimes.fajr.truncatedToMinute(),
                PrayerKey.SUNRISE to prayerTimes.sunrise.truncatedToMinute(),
                PrayerKey.DHUHR to prayerTimes.dhuhr.truncatedToMinute(),
                PrayerKey.ASR to prayerTimes.asr.truncatedToMinute(),
                PrayerKey.MAGHRIB to prayerTimes.maghrib.truncatedToMinute(),
                PrayerKey.ISHA to prayerTimes.isha.truncatedToMinute(),
            )
            if (times.values.any { it <= Instant.EPOCH }) {
                return@withContext ProviderResult.Failure(
                    ProviderError.MALFORMED_RESPONSE,
                    "Calculation produced a placeholder time (sun never reaches the required angle).",
                )
            }

            val sunnah = runCatching { com.batoulapps.adhan.SunnahTimes(prayerTimes) }.getOrNull()

            ProviderResult.Success(
                DayPrayerTimes(
                    date = request.date,
                    zoneId = zone,
                    times = times,
                    // Imsak is a discretionary pre-dawn stop, conventionally Fajr minus 10 minutes;
                    // the exact offset is a user setting and is labelled in the Ramadan screen.
                    imsak = prayerTimes.fajr.truncatedToMinute().minusSeconds(settings.imsakOffsetMinutes * 60L),
                    midnight = sunnah?.middleOfTheNight?.truncatedToMinute(),
                    lastThirdOfNight = sunnah?.lastThirdOfTheNight?.truncatedToMinute(),
                    source = PrayerSource.LOCAL_ADHAN,
                    methodLabel = settings.method.labelEn,
                    madhabLabel = settings.madhab.labelEn,
                    fetchedAt = Instant.now(),
                ),
            )
        } catch (t: Throwable) {
            ProviderResult.Failure(ProviderError.UNKNOWN, t.message ?: t::class.java.simpleName)
        }
    }
}

/**
 * AlAdhan API provider. Optional: the app never requires it. All requests are per-day GETs with
 * retry/backoff, and responses are validated before use.
 */
class AlAdhanProvider(
    private val api: AlAdhanApi,
) : PrayerTimesProvider {

    override val id: String = "aladhan"

    override suspend fun fetch(request: PrayerRequest): ProviderResult {
        val settings = request.settings
        val dateParam = "%02d-%02d-%04d".format(request.date.dayOfMonth, request.date.monthValue, request.date.year)
        return try {
            val response = withRetry { _ ->
                when (settings.providerMode) {
                    PrayerProviderMode.ALADHAN_ONLINE, PrayerProviderMode.ALADHAN_WITH_LOCAL_FALLBACK ->
                        if (!request.city.isNullOrBlank() && !request.country.isNullOrBlank()) {
                            api.timingsByCity(dateParam, request.city!!, request.country!!, settings.method.aladhanId, settings.madhab.aladhanSchool)
                        } else {
                            api.timingsByCoordinates(dateParam, request.latitude, request.longitude, settings.method.aladhanId, settings.madhab.aladhanSchool)
                        }
                    PrayerProviderMode.LOCAL ->
                        api.timingsByCoordinates(dateParam, request.latitude, request.longitude, settings.method.aladhanId, settings.madhab.aladhanSchool)
                }
            }
            if (response.code != 200 || response.data == null) {
                return ProviderResult.Failure(ProviderError.SERVER_ERROR, "AlAdhan returned status ${response.code} ${response.status}")
            }
            val parsed = parseDay(response.data, request)
            if (parsed == null) {
                ProviderResult.Failure(ProviderError.MALFORMED_RESPONSE, "AlAdhan response did not contain the expected timings")
            } else {
                ProviderResult.Success(parsed)
            }
        } catch (t: Throwable) {
            ProviderResult.Failure(t.toProviderError(), t.message)
        }
    }

    companion object {
        private val TIME_REGEX = Regex("""(\d{1,2}):(\d{2})""")

        /**
         * Parses an AlAdhan timings map into absolute instants. Values arrive as "04:44" or, for
         * calendar responses, "04:35 (+06)". Only the clock part is used; the date and the
         * location's zone determine the instant. Returns null when required keys are missing.
         */
        fun parseDay(data: AdhanTimingsData, request: PrayerRequest): DayPrayerTimes? {
            val zoneId = data.meta?.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: runCatching { ZoneId.of(request.zoneId) }.getOrNull()
                ?: return null

            fun instantOrNull(rawKey: String): Instant? {
                val raw = data.timings.entries.firstOrNull { it.key.equals(rawKey, ignoreCase = true) }?.value
                    ?: return null
                return parseClockToInstant(raw, request.date, zoneId)
            }

            val times = buildMap {
                instantOrNull("Fajr")?.let { put(PrayerKey.FAJR, it) }
                instantOrNull("Sunrise")?.let { put(PrayerKey.SUNRISE, it) }
                instantOrNull("Dhuhr")?.let { put(PrayerKey.DHUHR, it) }
                instantOrNull("Asr")?.let { put(PrayerKey.ASR, it) }
                (instantOrNull("Maghrib") ?: instantOrNull("Sunset"))?.let { put(PrayerKey.MAGHRIB, it) }
                instantOrNull("Isha")?.let { put(PrayerKey.ISHA, it) }
            }
            if (PrayerKey.obligatory.any { !times.containsKey(it) }) return null

            return DayPrayerTimes(
                date = request.date,
                zoneId = zoneId,
                times = times,
                imsak = instantOrNull("Imsak"),
                midnight = instantOrNull("Midnight"),
                lastThirdOfNight = instantOrNull("Lastthird"),
                source = PrayerSource.ALADHAN_API,
                methodLabel = data.meta?.method?.name ?: request.settings.method.labelEn,
                madhabLabel = when (data.meta?.school?.uppercase()) {
                    "HANAFI" -> AsrMadhab.HANAFI.labelEn
                    "SHAFI", "STANDARD" -> AsrMadhab.SHAFI.labelEn
                    else -> request.settings.madhab.labelEn
                },
                fetchedAt = Instant.now(),
            )
        }

        /** "04:35 (+06)" / "4:35" -> Instant on [date] in [zone]. */
        fun parseClockToInstant(raw: String, date: LocalDate, zone: ZoneId): Instant? {
            val match = TIME_REGEX.find(raw) ?: return null
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            if (minute !in 0..59) return null
            // AlAdhan can emit 24:00 for midnight-adjacent values; normalise to the next day 00:00.
            val normalisedHour = if (hour == 24) 0 else hour
            if (normalisedHour !in 0..23) return null
            val base = date.atStartOfDay(zone)
            val shifted = if (hour == 24) base.plusDays(1) else base
            return shifted.plusHours(normalisedHour.toLong()).plusMinutes(minute.toLong()).toInstant()
        }
    }
}

/** Truncates to the start of the minute (see the comment where it is used). */
internal fun java.util.Date.truncatedToMinute(): java.time.Instant =
    java.time.Instant.ofEpochMilli(Math.floorDiv(this.time, 60_000L) * 60_000L)

/** Fajr-style Imsak label helper: Fajr minus the user's imsak offset is computed by the provider. */
internal fun CalculationMethodId.displayName(bangla: Boolean): String = if (bangla) labelBn else labelEn

internal fun UserSettings.prayerAdjustments(): Map<PrayerKey, Int> = mapOf(
    PrayerKey.FAJR to adjustmentFajr,
    PrayerKey.SUNRISE to adjustmentSunrise,
    PrayerKey.DHUHR to adjustmentDhuhr,
    PrayerKey.ASR to adjustmentAsr,
    PrayerKey.MAGHRIB to adjustmentMaghrib,
    PrayerKey.ISHA to adjustmentIsha,
)
