package com.salahmate.app.data.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A surah's metadata plus its Arabic ayahs (bundled at build time). */
data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTranslation: String,
    val revelationType: String,
    val ayahCount: Int,
    val ayahs: List<Ayah>,
    /** Bengali/English name of the surah, provided locally so the list is usable offline. */
    val nameBengali: String = "",
)

data class Ayah(
    val numberInSurah: Int,
    val text: String,
    val juz: Int,
    val page: Int,
    val sajda: Boolean,
) {
    val globalNumber: Int get() = numberInSurah
}

/** Verified against https://api.alquran.cloud/v1/edition?language=bn (2 Bengali editions exist). */
object QuranEditions {
    const val ARABIC = "quran-uthmani"

    data class TranslationEdition(val id: String, val language: String, val label: String, val attribution: String)

    val BENGALI = listOf(
        TranslationEdition("bn.bengali", "bn", "মুহিউদ্দীন খান", "Muhiuddin Khan (Bengali)"),
        TranslationEdition("bn.hoque", "bn", "জহুরুল হক", "Zohurul Hoque (Bengali)"),
    )
    val ENGLISH = listOf(
        TranslationEdition("en.sahih", "en", "Saheeh International", "Saheeh International"),
        TranslationEdition("en.pickthall", "en", "Pickthall", "Marmaduke Pickthall"),
        TranslationEdition("en.yusufali", "en", "Yusuf Ali", "Abdullah Yusuf Ali"),
    )

    val all: List<TranslationEdition> = BENGALI + ENGLISH

    fun labelFor(id: String): String = all.firstOrNull { it.id == id }?.attribution ?: id

    /** Reciters verified against https://api.alquran.cloud/v1/edition?format=audio */
    data class Reciter(val id: String, val label: String, val bitrate: Int = 128)

    val RECITERS = listOf(
        Reciter("ar.alafasy", "Mishary Rashid Alafasy"),
        Reciter("ar.husary", "Mahmoud Khalil Al-Husary"),
        Reciter("ar.abdulbasitmurattal", "Abdul Basit (Murattal)"),
        Reciter("ar.minshawi", "Muhammad Siddiq Al-Minshawi"),
        Reciter("ar.mahermuaiqly", "Maher Al-Muaiqly"),
        Reciter("ar.shaatree", "Abu Bakr Ash-Shaatree"),
    )

    fun reciterLabel(id: String): String = RECITERS.firstOrNull { it.id == id }?.label ?: id

    /** CDN pattern verified working: https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3 */
    fun ayahAudioUrl(reciterId: String, globalAyahNumber: Int, bitrate: Int = 128): String =
        "https://cdn.islamic.network/quran/audio/$bitrate/$reciterId/$globalAyahNumber.mp3"
}

// ------------------------------------------------------------- bundled JSON --

@Serializable
internal data class BundledQuran(
    val edition: BundledEdition = BundledEdition(),
    val surahs: List<BundledSurah> = emptyList(),
)

@Serializable
internal data class BundledEdition(
    val identifier: String = QuranEditions.ARABIC,
    val name: String = "",
    val source: String = "",
)

@Serializable
internal data class BundledSurah(
    val number: Int = 0,
    val name: String = "",
    val englishName: String = "",
    val englishNameTranslation: String = "",
    val revelationType: String = "Meccan",
    val numberOfAyahs: Int = 0,
    val ayahs: List<BundledAyah> = emptyList(),
)

@Serializable
internal data class BundledAyah(
    val n: Int = 0,
    val t: String = "",
    val j: Int = 0,
    val p: Int = 0,
    @SerialName("s") val sajda: Boolean = false,
)

// ------------------------------------------------------- API response models --

@Serializable
data class CloudEditionResponse(
    val code: Int = 0,
    val status: String = "",
    val data: CloudEditionData? = null,
)

@Serializable
data class CloudEditionData(
    val number: Int = 0,
    val name: String = "",
    val englishName: String = "",
    val englishNameTranslation: String = "",
    val revelationType: String = "",
    val numberOfAyahs: Int = 0,
    val ayahs: List<CloudAyah> = emptyList(),
    val edition: CloudEditionInfo? = null,
)

@Serializable
data class CloudAyah(
    val number: Int = 0,
    val text: String = "",
    val numberInSurah: Int = 0,
    val juz: Int = 0,
    val page: Int = 0,
    val audio: String? = null,
    @SerialName("audioSecondary") val audioSecondary: List<String> = emptyList(),
    val sajda: Boolean = false,
)

@Serializable
data class CloudEditionInfo(
    val identifier: String = "",
    val language: String = "",
    val name: String = "",
    val englishName: String = "",
    val type: String = "",
    val format: String = "",
)
