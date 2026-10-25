package com.salahmate.app.data.prayer

import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.DayPrayerTimes
import java.time.LocalDate

/** Everything a provider needs to compute or fetch one day. Immutable. */
data class PrayerRequest(
    val date: LocalDate,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val city: String?,
    val country: String?,
    val settings: UserSettings,
)

/** Either real data or a typed failure the UI can explain to the user. */
sealed interface ProviderResult {
    data class Success(val day: DayPrayerTimes) : ProviderResult
    data class Failure(val reason: ProviderError, val message: String? = null) : ProviderResult
}

enum class ProviderError {
    NETWORK_UNAVAILABLE,
    TIMEOUT,
    RATE_LIMITED,
    SERVER_ERROR,
    MALFORMED_RESPONSE,
    UNKNOWN,
}

/**
 * A prayer-time data source. The UI and domain layers only touch this interface, so a different
 * backend can be added without changing screens.
 */
interface PrayerTimesProvider {
    val id: String
    suspend fun fetch(request: PrayerRequest): ProviderResult
}
