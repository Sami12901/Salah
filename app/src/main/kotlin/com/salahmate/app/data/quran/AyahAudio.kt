package com.salahmate.app.data.quran

import java.io.File

/** Where a played ayah came from — surfaced in the reader so offline playback is honest. */
enum class AudioSource { LOCAL_FILE, STREAM }

/**
 * Ayah audio addressing, kept free of Android types so the URL layout, the cache path and the
 * ayah-to-ayah stepping can be verified in plain JVM tests.
 *
 * Streaming URL layout (verified against the AlQuran.cloud CDN):
 * `https://cdn.islamic.network/quran/audio/128/{reciter}/{globalAyahNumber}.mp3`
 */
object AyahAudio {

    const val DEFAULT_BITRATE = 128

    fun url(reciterId: String, globalAyahNumber: Int, bitrate: Int = DEFAULT_BITRATE): String =
        QuranEditions.ayahAudioUrl(reciterId, globalAyahNumber, bitrate)

    fun fileName(globalAyahNumber: Int): String = "$globalAyahNumber.mp3"

    /** `<filesDir>/quran/audio/<reciter>/<n>.mp3` — app-private, removed with the app. */
    fun localFile(filesDir: File, reciterId: String, globalAyahNumber: Int): File =
        File(audioDir(filesDir, reciterId), fileName(globalAyahNumber))

    fun audioDir(filesDir: File, reciterId: String): File =
        File(File(filesDir, "quran"), "audio/$reciterId")

    /** Next ayah within the surah, or null when the surah has finished. */
    fun nextAyah(current: Int, ayahCount: Int): Int? =
        if (current >= 1 && current < ayahCount) current + 1 else null

    /** Previous ayah, clamped at the first ayah so seeking back never goes out of range. */
    fun previousAyah(current: Int): Int = (current - 1).coerceAtLeast(1)

    fun sourceFor(localExists: Boolean): AudioSource =
        if (localExists) AudioSource.LOCAL_FILE else AudioSource.STREAM
}
