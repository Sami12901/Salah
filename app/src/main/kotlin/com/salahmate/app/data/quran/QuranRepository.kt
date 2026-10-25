package com.salahmate.app.data.quran

import android.content.Context
import com.salahmate.app.core.db.DailyReadingEntity
import com.salahmate.app.core.db.QuranBookmarkEntity
import com.salahmate.app.core.db.QuranDao
import com.salahmate.app.core.db.ReadingHistoryEntity
import com.salahmate.app.core.db.ReadingPositionEntity
import com.salahmate.app.core.net.AppJson
import com.salahmate.app.core.net.toProviderError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Translation text for one surah, with provenance so the UI can attribute it. */
data class SurahTranslation(
    val surah: Int,
    val editionId: String,
    val ayahs: Map<Int, String>,
    val fromCache: Boolean,
    val attribution: String,
)

sealed interface TranslationResult {
    data class Success(val translation: SurahTranslation) : TranslationResult
    data class Failure(val reason: com.salahmate.app.data.prayer.ProviderError, val message: String? = null) : TranslationResult
}

sealed interface SurahResult {
    data class Success(val surah: Surah) : SurahResult
    data class Failure(val message: String) : SurahResult
}

/**
 * Quran content access.
 *
 * Arabic text and surah metadata are bundled in `assets/quran/quran_uthmani.json` (1.55 MB,
 * 114 surahs, 6236 ayahs, validated by tools/fetch_quran_text.py), so the reader opens instantly
 * and works offline after a cold start, force-stop, reboot or in airplane mode.
 *
 * Translations and recitation audio are *optional* network content: fetched on demand, written to
 * app-private storage, and thereafter served from disk. Nothing is downloaded automatically on
 * launch, and the whole Quran is never re-downloaded silently.
 */
class QuranRepository(
    private val context: Context,
    private val dao: QuranDao,
    private val api: AlQuranCloudApi,
) {
    private val loadMutex = Mutex()
    @Volatile private var cache: Map<Int, Surah>? = null

    // ---------------------------------------------------------------- Arabic --

    suspend fun surahIndex(): List<Surah> = withContext(Dispatchers.IO) {
        loadAll().values.sortedBy { it.number }
    }

    suspend fun surah(number: Int): SurahResult = withContext(Dispatchers.IO) {
        val all = loadAll()
        all[number]?.let { SurahResult.Success(it) }
            ?: SurahResult.Failure("Surah $number is not in the bundled Quran text")
    }

    /** Total ayah count for progress maths; 6236 for the Uthmani text. */
    suspend fun totalAyahs(): Int = withContext(Dispatchers.IO) { loadAll().values.sumOf { it.ayahCount } }

    private suspend fun loadAll(): Map<Int, Surah> {
        cache?.let { return it }
        return loadMutex.withLock {
            cache?.let { return it }
            val parsed = withContext(Dispatchers.IO) {
                val text = context.assets.open("quran/quran_uthmani.json").bufferedReader().use { it.readText() }
                val bundled = AppJson.decodeFromString<BundledQuran>(text)
                bundled.surahs.associate { s ->
                    s.number to Surah(
                        number = s.number,
                        nameArabic = s.name,
                        nameEnglish = s.englishName,
                        nameTranslation = s.englishNameTranslation,
                        revelationType = s.revelationType,
                        ayahCount = s.ayahs.size,
                        ayahs = s.ayahs.map { a ->
                            Ayah(numberInSurah = a.n, text = a.t, juz = a.j, page = a.p, sajda = a.sajda)
                        },
                        nameBengali = SurahNamesBn.forNumber(s.number),
                    )
                }
            }
            cache = parsed
            parsed
        }
    }

    // ---------------------------------------------------------- Translation ---

    private fun translationFile(edition: String, surah: Int): File =
        File(File(context.filesDir, "quran/translations/$edition"), "$surah.json")

    suspend fun cachedTranslationEdition(surah: Int, edition: String): SurahTranslation? =
        withContext(Dispatchers.IO) { readTranslationFile(edition, surah) }

    /** Loads a translation from disk, fetching and caching it once if it is not there yet. */
    suspend fun translation(surah: Int, edition: String, allowNetwork: Boolean = true): TranslationResult =
        withContext(Dispatchers.IO) {
            readTranslationFile(edition, surah)?.let { return@withContext TranslationResult.Success(it) }
            if (!allowNetwork) {
                return@withContext TranslationResult.Failure(
                    com.salahmate.app.data.prayer.ProviderError.NETWORK_UNAVAILABLE,
                    "Translation not downloaded",
                )
            }
            try {
                val response = com.salahmate.app.core.net.withRetry { api.surahByEdition(surah, edition) }
                val ayahs = response.data?.ayahs ?: return@withContext TranslationResult.Failure(
                    com.salahmate.app.data.prayer.ProviderError.MALFORMED_RESPONSE, "No ayahs in response",
                )
                val map = ayahs.associate { it.numberInSurah to it.text }
                val translation = SurahTranslation(
                    surah = surah,
                    editionId = edition,
                    ayahs = map,
                    fromCache = false,
                    attribution = QuranEditions.labelFor(edition),
                )
                runCatching { writeTranslationFile(edition, surah, map) }
                TranslationResult.Success(translation)
            } catch (t: Throwable) {
                TranslationResult.Failure(t.toProviderError(), t.message)
            }
        }

    private fun readTranslationFile(edition: String, surah: Int): SurahTranslation? {
        val file = translationFile(edition, surah)
        if (!file.exists()) return null
        return runCatching {
            val obj = AppJson.parseToJsonElement(file.readText()).let { it }
            val root = obj as kotlinx.serialization.json.JsonObject
            val ayahs = (root["ayahs"] as? kotlinx.serialization.json.JsonObject)?.mapValues { entry ->
                (entry.value as kotlinx.serialization.json.JsonPrimitive).content
            } ?: return null
            SurahTranslation(
                surah = surah,
                editionId = edition,
                ayahs = ayahs.mapKeys { it.key.toInt() },
                fromCache = true,
                attribution = QuranEditions.labelFor(edition),
            )
        }.getOrNull()
    }

    private fun writeTranslationFile(edition: String, surah: Int, ayahs: Map<Int, String>) {
        val file = translationFile(edition, surah)
        file.parentFile?.mkdirs()
        val obj = kotlinx.serialization.json.JsonObject(
            mapOf(
                "edition" to kotlinx.serialization.json.JsonPrimitive(edition),
                "ayahs" to kotlinx.serialization.json.JsonObject(ayahs.mapKeys { it.key.toString() }.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value) }),
            ),
        )
        file.writeText(obj.toString())
    }

    /** Removes cached translations/audio for one surah or everything (user-initiated cleanup). */
    suspend fun deleteCachedDownloads(edition: String? = null, surah: Int? = null) = withContext(Dispatchers.IO) {
        when {
            edition != null && surah != null -> translationFile(edition, surah).delete()
            edition != null -> File(context.filesDir, "quran/translations/$edition").deleteRecursively()
            else -> File(context.filesDir, "quran").deleteRecursively()
        }
        Unit
    }

    suspend fun storageUsageBytes(): Long = withContext(Dispatchers.IO) {
        File(context.filesDir, "quran").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    // ---------------------------------------------------) Bookmarks/position --

    val bookmarks: Flow<List<QuranBookmarkEntity>> = dao.bookmarks()
    val readingPosition: Flow<ReadingPositionEntity?> = dao.readingPosition()
    val readingHistory: Flow<List<ReadingHistoryEntity>> = dao.readingHistory()
    val recentDailyReading: Flow<List<DailyReadingEntity>> = dao.recentDailyReading(30)

    suspend fun isBookmarked(surah: Int, ayah: Int): Boolean = dao.bookmarkAt(surah, ayah) != null

    suspend fun addBookmark(surah: Int, ayah: Int, note: String? = null) {
        dao.insertBookmark(QuranBookmarkEntity(surah = surah, ayah = ayah, note = note, createdAt = System.currentTimeMillis()))
    }

    suspend fun removeBookmark(surah: Int, ayah: Int) = dao.deleteBookmark(surah, ayah)

    suspend fun saveReadingPosition(surah: Int, ayah: Int) {
        val now = System.currentTimeMillis()
        dao.saveReadingPosition(ReadingPositionEntity(id = 1, surah = surah, ayah = ayah, updatedAt = now))
        val updated = dao.updateHistory(surah = surah, ayah = ayah, updatedAt = now)
        if (updated == 0) {
            dao.insertHistory(ReadingHistoryEntity(surah = surah, lastAyah = ayah, updatedAt = now))
        }
    }

    /** Adds to today's ayah counter; called when the reader advances between ayahs. */
    suspend fun addReadAyahs(count: Int, date: String = LocalDate.now().toString()) {
        if (count <= 0) return
        val current = dao.dailyReading(date)?.ayahsRead ?: 0
        dao.upsertDailyReading(DailyReadingEntity(date = date, ayahsRead = current + count))
    }

    suspend fun searchArabic(query: String): List<Pair<Surah, Ayah>> = withContext(Dispatchers.IO) {
        val q = normalizeArabic(query)
        if (q.length < 2) return@withContext emptyList()
        loadAll().values.flatMap { surah ->
            surah.ayahs.filter { normalizeArabic(it.text).contains(q) }.map { surah to it }
        }.take(200)
    }

    /** Strips Arabic diacritics so search matches undiacritised queries. */
    private fun normalizeArabic(input: String): String {
        val diacritics = charArrayOf(
            '\u0610', '\u0611', '\u0612', '\u0613', '\u0614', '\u0615', '\u0616', '\u0617', '\u0618', '\u0619', '\u061A',
            '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652', '\u0653', '\u0654', '\u0655',
            '\u0656', '\u0657', '\u0658', '\u0670', '\u06D6', '\u06D7', '\u06D8', '\u06D9', '\u06DA', '\u06DB',
            '\u06DC', '\u06DF', '\u06E0', '\u06E1', '\u06E2', '\u06E3', '\u06E4', '\u06E7', '\u06E8', '\u06EA',
            '\u06EB', '\u06EC', '\u06ED', '\uFEFF',
        ).toSet()
        return input.filterNot { it in diacritics }
            .replace('\u0623', '\u0627')
            .replace('\u0625', '\u0627')
            .replace('\u0622', '\u0627')
            .replace('\u0649', '\u064A')
            .replace('\u0629', '\u0647')
            .trim()
    }
}
