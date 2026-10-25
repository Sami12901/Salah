package com.salahmate.app.data.prayer

import com.salahmate.app.core.db.PrayerCacheDao
import com.salahmate.app.core.db.PrayerCacheEntity
import com.salahmate.app.core.prefs.PrayerProviderMode
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the UI needs to render a schedule plus an honest statement of where it came from. */
data class PrayerDayState(
    val day: DayPrayerTimes,
    val providerError: ProviderError? = null,
    val providerErrorMessage: String? = null,
    /** True when the schedule shown is a cached copy rather than a fresh fetch. */
    val fromCache: Boolean = false,
    /** Set when the online provider failed and the local engine produced the times instead. */
    val fellBackToLocal: Boolean = false,
)

/**
 * Coordinates the two providers, applies user adjustments exactly once, caches results and always
 * degrades to a usable schedule instead of an error screen when that is possible.
 *
 * Cache key = date + rounded coordinates + method + madhab + provider, so a cached row can never be
 * shown for a different configuration.
 */
class PrayerTimesRepository(
    private val localProvider: PrayerTimesProvider,
    private val onlineProvider: PrayerTimesProvider,
    private val cacheDao: PrayerCacheDao,
    private val json: Json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true },
) {

    fun dayFlow(settings: UserSettings, date: LocalDate): Flow<PrayerDayState> =
        kotlinx.coroutines.flow.flow { emit(loadDay(settings, date)) }.distinctUntilChanged()

    suspend fun loadDay(settings: UserSettings, date: LocalDate, allowNetwork: Boolean = true): PrayerDayState {
        val request = buildRequest(settings, date)
        val providerKey = settings.providerMode.name

        // 1. Fresh online fetch when the user explicitly asked for the API.
        if (settings.providerMode != PrayerProviderMode.LOCAL && allowNetwork) {
            when (val online = onlineProvider.fetch(request)) {
                is ProviderResult.Success -> {
                    val adjusted = online.day.withAdjustments(settings.prayerAdjustments())
                    saveCache(settings, date, adjusted)
                    return PrayerDayState(day = adjusted, fromCache = false)
                }
                is ProviderResult.Failure -> {
                    if (settings.providerMode == PrayerProviderMode.ALADHAN_ONLINE) {
                        // Do not pretend data arrived. Show cache if we have it, otherwise the error.
                        cached(settings, date)?.let { cachedDay ->
                            val adjusted = cachedDay.withAdjustments(settings.prayerAdjustments())
                            return PrayerDayState(
                                day = adjusted.copy(source = PrayerSource.CACHED),
                                providerError = online.reason,
                                providerErrorMessage = online.message,
                                fromCache = true,
                            )
                        }
                        val fallbackLocal = localProvider.fetch(request)
                        if (fallbackLocal is ProviderResult.Success) {
                            // Even in API-only mode the user still gets a labelled local schedule.
                            return PrayerDayState(
                                day = fallbackLocal.day.withAdjustments(settings.prayerAdjustments())
                                    .copy(source = PrayerSource.LOCAL_ADHAN),
                                providerError = online.reason,
                                providerErrorMessage = online.message,
                                fellBackToLocal = true,
                            )
                        }
                        return PrayerDayState(
                            day = emptyDay(settings, date),
                            providerError = online.reason,
                            providerErrorMessage = online.message,
                        )
                    }
                    // ALADHAN_WITH_LOCAL_FALLBACK: fall through to local calculation below.
                    val local = localProvider.fetch(request)
                    if (local is ProviderResult.Success) {
                        val adjusted = local.day.withAdjustments(settings.prayerAdjustments())
                        return PrayerDayState(
                            day = adjusted,
                            providerError = online.reason,
                            providerErrorMessage = online.message,
                            fellBackToLocal = true,
                        )
                    }
                    return PrayerDayState(
                        day = emptyDay(settings, date),
                        providerError = online.reason,
                        providerErrorMessage = online.message,
                    )
                }
            }
        }

        // 2. Offline-first local calculation.
        return when (val local = localProvider.fetch(request)) {
            is ProviderResult.Success -> {
                val adjusted = local.day.withAdjustments(settings.prayerAdjustments())
                saveCache(settings, date, adjusted)
                PrayerDayState(day = adjusted, fromCache = false)
            }
            is ProviderResult.Failure -> {
                val cachedDay = cached(settings, date)
                if (cachedDay != null) {
                    PrayerDayState(
                        day = cachedDay.withAdjustments(settings.prayerAdjustments()).copy(source = PrayerSource.CACHED),
                        providerError = local.reason,
                        providerErrorMessage = local.message,
                        fromCache = true,
                    )
                } else {
                    PrayerDayState(
                        day = emptyDay(settings, date),
                        providerError = local.reason,
                        providerErrorMessage = local.message,
                    )
                }
            }
        }
    }

    /** Tomorrow's and yesterday's schedules, used for "next prayer after Isha" and night windows. */
    suspend fun loadNeighbours(settings: UserSettings, date: LocalDate): Pair<DayPrayerTimes?, DayPrayerTimes?> {
        val yesterday = loadDay(settings, date.minusDays(1), allowNetwork = false).day.takeIf { it.hasAllObligatory }
        val tomorrow = loadDay(settings, date.plusDays(1), allowNetwork = false).day.takeIf { it.hasAllObligatory }
        return yesterday to tomorrow
    }

    suspend fun monthSchedule(settings: UserSettings, year: Int, month: Int): List<DayPrayerTimes> {
        val first = LocalDate.of(year, month, 1)
        return (1..first.lengthOfMonth()).map { d ->
            loadDay(settings, LocalDate.of(year, month, d), allowNetwork = false).day
        }
    }

    private fun emptyDay(settings: UserSettings, date: LocalDate) = DayPrayerTimes(
        date = date,
        zoneId = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault()),
        times = emptyMap(),
        methodLabel = settings.method.labelEn,
        madhabLabel = settings.madhab.labelEn,
        source = PrayerSource.LOCAL_ADHAN,
    )

    private fun buildRequest(settings: UserSettings, date: LocalDate): PrayerRequest = PrayerRequest(
        date = date,
        latitude = settings.latitude,
        longitude = settings.longitude,
        zoneId = settings.timeZoneId,
        city = settings.city,
        country = settings.country,
        settings = settings,
    )

    private suspend fun cached(settings: UserSettings, date: LocalDate): DayPrayerTimes? {
        val row = cacheDao.get(
            date = date.toString(),
            latKey = settings.latitude.coordKey(),
            lonKey = settings.longitude.coordKey(),
            method = settings.method.name,
            madhab = settings.madhab.name,
            provider = if (settings.providerMode == PrayerProviderMode.LOCAL) "local_adhan" else "aladhan",
        ) ?: return null
        return runCatching { json.decodeFromString<CachedDay>(row.payloadJson).toDay() }.getOrNull()
    }

    private suspend fun saveCache(settings: UserSettings, date: LocalDate, day: DayPrayerTimes) {
        runCatching {
            cacheDao.put(
                PrayerCacheEntity(
                    date = date.toString(),
                    latKey = settings.latitude.coordKey(),
                    lonKey = settings.longitude.coordKey(),
                    method = settings.method.name,
                    madhab = settings.madhab.name,
                    provider = day.source.cacheProviderKey(),
                    payloadJson = json.encodeToString(CachedDay.from(day)),
                    fetchedAt = Instant.now().toEpochMilli(),
                ),
            )
        }
    }

    /** Housekeeping: keep one month of cache rows. */
    suspend fun pruneCache(olderThan: Instant) {
        runCatching { cacheDao.prune(olderThan.toEpochMilli()) }
    }
}

internal fun Double.coordKey(): String = String.format("%.4f", this)

private fun PrayerSource.cacheProviderKey(): String = when (this) {
    PrayerSource.ALADHAN_API -> "aladhan"
    PrayerSource.LOCAL_ADHAN, PrayerSource.CACHED -> "local_adhan"
}

/** Compact, version-tolerant cache representation (instants stored as epoch millis). */
@Serializable
internal data class CachedDay(
    val date: String,
    val zone: String,
    val times: Map<String, Long>,
    val imsak: Long? = null,
    val midnight: Long? = null,
    val lastThird: Long? = null,
    val method: String = "",
    val madhab: String = "",
    val source: String = "LOCAL_ADHAN",
    val fetchedAt: Long = 0,
) {
    fun toDay(): DayPrayerTimes = DayPrayerTimes(
        date = LocalDate.parse(date),
        zoneId = ZoneId.of(zone),
        times = times.mapNotNull { (k, v) -> PrayerKey.from(k)?.let { it to Instant.ofEpochMilli(v) } }.toMap(),
        imsak = imsak?.let { Instant.ofEpochMilli(it) },
        midnight = midnight?.let { Instant.ofEpochMilli(it) },
        lastThirdOfNight = lastThird?.let { Instant.ofEpochMilli(it) },
        source = PrayerSource.CACHED,
        methodLabel = method,
        madhabLabel = madhab,
        fetchedAt = Instant.ofEpochMilli(fetchedAt),
    )

    companion object {
        fun from(day: DayPrayerTimes) = CachedDay(
            date = day.date.toString(),
            zone = day.zoneId.id,
            times = day.times.mapKeys { it.key.id }.mapValues { it.value.toEpochMilli() },
            imsak = day.imsak?.toEpochMilli(),
            midnight = day.midnight?.toEpochMilli(),
            lastThird = day.lastThirdOfNight?.toEpochMilli(),
            method = day.methodLabel,
            madhab = day.madhabLabel,
            source = day.source.name,
            fetchedAt = (day.fetchedAt ?: Instant.now()).toEpochMilli(),
        )
    }
}
