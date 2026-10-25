package com.salahmate.app.domain.prayer

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The five obligatory prayers plus sunrise (shown for Fajr's end / Duha). */
enum class PrayerKey(val id: String, val labelEn: String, val labelBn: String, val isObligatory: Boolean) {
    FAJR("FAJR", "Fajr", "ফজর", true),
    SUNRISE("SUNRISE", "Sunrise", "সূর্যোদয়", false),
    DHUHR("DHUHR", "Dhuhr", "যোহর", true),
    ASR("ASR", "Asr", "আসর", true),
    MAGHRIB("MAGHRIB", "Maghrib", "মাগরিব", true),
    ISHA("ISHA", "Isha", "এশা", true),
    ;

    companion object {
        fun from(id: String?): PrayerKey? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }

        /** The five daily obligatory prayers in order. */
        val obligatory: List<PrayerKey> = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

enum class PrayerSource(val labelEn: String, val labelBn: String) {
    LOCAL_ADHAN("On-device calculation", "ডিভাইসে গণনা"),
    ALADHAN_API("AlAdhan API", "আল-আধান এপিআই"),
    CACHED("Cached copy", "সংরক্ষিত কপি"),
}

/**
 * A single day of prayer times for one location, in absolute time ([Instant]) plus the zone they
 * are displayed in. Instants — not wall-clock strings — so a timezone change cannot silently
 * shift a schedule.
 */
data class DayPrayerTimes(
    val date: LocalDate,
    val zoneId: ZoneId,
    val times: Map<PrayerKey, Instant>,
    val imsak: Instant? = null,
    val midnight: Instant? = null,
    val lastThirdOfNight: Instant? = null,
    val source: PrayerSource = PrayerSource.LOCAL_ADHAN,
    val methodLabel: String = "",
    val madhabLabel: String = "",
    val adjustments: Map<PrayerKey, Int> = emptyMap(),
    val fetchedAt: Instant? = null,
) {
    fun time(key: PrayerKey): Instant? = times[key]

    /** Maghrib is sunset for the purposes of this app; both labels are surfaced separately in UI. */
    val sunset: Instant? get() = times[PrayerKey.MAGHRIB]

    val hasAllObligatory: Boolean get() = PrayerKey.obligatory.all { times.containsKey(it) }

    /** Apply user minute adjustments on top of what a provider returned. */
    fun withAdjustments(adjustments: Map<PrayerKey, Int>): DayPrayerTimes {
        if (adjustments.isEmpty() || adjustments.values.all { it == 0 }) {
            return if (adjustments.isEmpty()) this else copy(adjustments = adjustments)
        }
        val shifted = times.mapValues { (key, instant) ->
            instant.plus(Duration.ofMinutes(adjustments[key]?.toLong() ?: 0L))
        }
        return copy(
            times = shifted,
            adjustments = adjustments,
            imsak = imsak?.plus(Duration.ofMinutes(adjustments[PrayerKey.FAJR]?.toLong() ?: 0L)),
        )
    }
}

/** The prayer that is "current" and the one that comes next, with the gap between them. */
data class PrayerProgress(
    val current: PrayerKey?,
    val currentStart: Instant?,
    val next: PrayerKey,
    val nextTime: Instant,
    val remaining: Duration,
    val isNextTomorrow: Boolean,
)

/**
 * Pure scheduling helpers. Free of Android types so they can be unit-tested with fixed clocks and
 * arbitrary zones (see PrayerScheduleTest).
 */
object PrayerSchedule {

    /**
     * Which obligatory prayer window [now] falls in, and which prayer comes next.
     *
     * A "day" for this purpose starts at Fajr. Between midnight and Fajr the current prayer is
     * yesterday's Isha, which is why [yesterday] can be supplied.
     */
    fun progress(
        today: DayPrayerTimes,
        now: Instant,
        tomorrow: DayPrayerTimes? = null,
        yesterday: DayPrayerTimes? = null,
    ): PrayerProgress? {
        val ordered = PrayerKey.obligatory
        val todayTimes = ordered.mapNotNull { k -> today.time(k)?.let { k to it } }
        if (todayTimes.size != ordered.size) return null

        val fajrToday = today.time(PrayerKey.FAJR)!!

        // Case 1: before today's Fajr -> yesterday's Isha is the current window.
        if (now.isBefore(fajrToday)) {
            return PrayerProgress(
                current = PrayerKey.ISHA,
                currentStart = yesterday?.time(PrayerKey.ISHA),
                next = PrayerKey.FAJR,
                nextTime = fajrToday,
                remaining = Duration.between(now, fajrToday),
                isNextTomorrow = false,
            )
        }

        // Case 2: within today, find the last prayer whose time has passed.
        var currentIndex = -1
        for (i in todayTimes.indices) {
            if (!now.isBefore(todayTimes[i].second)) currentIndex = i
        }
        if (currentIndex == -1) return null

        val nextIndex = currentIndex + 1
        if (nextIndex < todayTimes.size) {
            val (nextKey, nextAt) = todayTimes[nextIndex]
            return PrayerProgress(
                current = todayTimes[currentIndex].first,
                currentStart = todayTimes[currentIndex].second,
                next = nextKey,
                nextTime = nextAt,
                remaining = Duration.between(now, nextAt),
                isNextTomorrow = false,
            )
        }

        // Case 3: after today's Isha -> tomorrow's Fajr.
        val tomorrowFajr = tomorrow?.time(PrayerKey.FAJR)
        return if (tomorrowFajr != null) {
            PrayerProgress(
                current = PrayerKey.ISHA,
                currentStart = todayTimes.last().second,
                next = PrayerKey.FAJR,
                nextTime = tomorrowFajr,
                remaining = Duration.between(now, tomorrowFajr),
                isNextTomorrow = true,
            )
        } else {
            // Without tomorrow's data the window is still open; approximate for display only.
            val nominal = todayTimes.last().second.plus(Duration.ofHours(9))
            PrayerProgress(
                current = PrayerKey.ISHA,
                currentStart = todayTimes.last().second,
                next = PrayerKey.FAJR,
                nextTime = nominal,
                remaining = Duration.between(now, nominal),
                isNextTomorrow = true,
            )
        }
    }

    /** Progress through the current prayer window, 0..1, for the ring indicator. */
    fun windowProgress(progress: PrayerProgress): Float {
        val start = progress.currentStart ?: return 0f
        val total = Duration.between(start, progress.nextTime).seconds.toDouble()
        if (total <= 0) return 0f
        val elapsed = total - progress.remaining.seconds
        return (elapsed / total).coerceIn(0.0, 1.0).toFloat()
    }
}
