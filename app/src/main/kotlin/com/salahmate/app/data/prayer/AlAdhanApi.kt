package com.salahmate.app.data.prayer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * AlAdhan Prayer Times API — https://aladhan.com/prayer-times-api
 *
 * `method` selects the calculation method (1 = University of Islamic Sciences, Karachi) and
 * `school` selects the Asr shadow length (0 = standard/Shafi, 1 = Hanafi). These two are always
 * sent separately; the app never conflates them.
 */
interface AlAdhanApi {

    @GET("v1/timings/{date}")
    suspend fun timingsByCoordinates(
        @Path("date") date: String,             // DD-MM-YYYY
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int,
    ): AdhanTimingsResponse

    @GET("v1/timingsByCity/{date}")
    suspend fun timingsByCity(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int,
        @Query("school") school: Int,
    ): AdhanTimingsResponse

    @GET("v1/calendar/{year}/{month}")
    suspend fun calendarByCoordinates(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int,
    ): AdhanCalendarResponse
}

@Serializable
data class AdhanTimingsResponse(
    val code: Int = 0,
    val status: String = "",
    val data: AdhanTimingsData? = null,
)

@Serializable
data class AdhanTimingsData(
    val timings: Map<String, String> = emptyMap(),
    val date: AdhanDateInfo? = null,
    val meta: AdhanMeta? = null,
)

@Serializable
data class AdhanMeta(
    val timezone: String? = null,
    val method: AdhanMethodInfo? = null,
    @SerialName("latitudeAdjustmentMethod") val latitudeAdjustmentMethod: String? = null,
    val school: String? = null,
)

@Serializable
data class AdhanMethodInfo(
    val id: Int = 0,
    val name: String? = null,
)

@Serializable
data class AdhanDateInfo(
    val readable: String? = null,
    val timestamp: String? = null,
    val hijri: AdhanHijri? = null,
    val gregorian: AdhanGregorian? = null,
)

@Serializable
data class AdhanHijri(
    val date: String? = null,
    val day: String? = null,
    val year: String? = null,
    val month: AdhanHijriMonth? = null,
    val holidays: List<String> = emptyList(),
)

@Serializable
data class AdhanHijriMonth(
    val number: Int = 0,
    val en: String? = null,
    val ar: String? = null,
)

@Serializable
data class AdhanGregorian(
    val date: String? = null,
    val day: String? = null,
    val month: AdhanGregorianMonth? = null,
    val year: String? = null,
)

@Serializable
data class AdhanGregorianMonth(
    val number: Int = 0,
    val en: String? = null,
)

@Serializable
data class AdhanCalendarResponse(
    val code: Int = 0,
    val status: String = "",
    val data: List<AdhanTimingsData> = emptyList(),
)
