package com.salahmate.app.data.quran

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * AlQuran.cloud API — https://alquran.cloud/api
 *
 * Only used for translations and recitation audio; the Arabic text ships with the app so the
 * reader works with no network at all.
 */
interface AlQuranCloudApi {

    @GET("v1/surah/{number}/{edition}")
    suspend fun surahByEdition(
        @Path("number") surahNumber: Int,
        @Path("edition") edition: String,
    ): CloudEditionResponse

    @GET("v1/edition")
    suspend fun editions(): CloudEditionListResponse
}

@kotlinx.serialization.Serializable
data class CloudEditionListResponse(
    val code: Int = 0,
    val status: String = "",
    val data: List<CloudEditionInfo> = emptyList(),
)
