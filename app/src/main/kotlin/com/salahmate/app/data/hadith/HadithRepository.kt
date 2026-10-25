package com.salahmate.app.data.hadith

import android.content.Context
import com.salahmate.app.core.net.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class HadithMeta(
    val id: String = "",
    val name: String = "",
    val source: String = "",
    val license: String = "",
    val languages: List<String> = emptyList(),
    val hadithCount: Int = 0,
)

@Serializable
data class HadithGrade(
    val grade: String = "",
    val scholar: String = "",
)

@Serializable
data class HadithRef(
    val book: Int = 0,
    val hadith: Int = 0,
)

@Serializable
data class Hadith(
    val number: Int = 0,
    val arabic: String = "",
    val bengali: String = "",
    val english: String = "",
    val grades: List<HadithGrade> = emptyList(),
    val reference: HadithRef = HadithRef(),
    val book: Int = 0,
    val bookName: String = "",
)

@Serializable
data class HadithCollection(
    val collection: HadithMeta = HadithMeta(),
    val sections: Map<String, String> = emptyMap(),
    val hadiths: List<Hadith> = emptyList(),
)

@Serializable
data class HadithManifest(
    val bundled: List<HadithMeta> = emptyList(),
    val downloadable: List<HadithMeta> = emptyList(),
)

/**
 * Hadith access.
 *
 * Two of the smaller collections (an-Nawawi's Forty and the Forty Hadith Qudsi) are bundled so the
 * section works offline. Larger collections are optional, user-initiated downloads from the
 * public-domain hadith-api CDN; they are cached on disk and only re-fetched on explicit request.
 *
 * Grading is shown exactly as the upstream dataset provides it, and omitted when absent. The app
 * does not infer, translate or upgrade a grading.
 */
class HadithRepository(private val context: Context) {

    private val memory = mutableMapOf<String, HadithCollection>()
    private val base = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions"

    suspend fun manifest(): HadithManifest = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.assets.open("hadith/manifest.json").bufferedReader().use { it.readText() }
            AppJson.decodeFromString<HadithManifest>(text)
        }.getOrDefault(HadithManifest())
    }

    suspend fun bundledCollection(id: String): HadithCollection? = withContext(Dispatchers.IO) {
        memory[id]?.let { return@withContext it }
        runCatching {
            val text = context.assets.open("hadith/$id.json").bufferedReader().use { it.readText() }
            val parsed = AppJson.decodeFromString<HadithCollection>(text)
            memory[id] = parsed
            parsed
        }.getOrNull()
    }

    /** Reads a previously downloaded collection, if present. */
    suspend fun downloadedCollection(id: String): HadithCollection? = withContext(Dispatchers.IO) {
        memory[id]?.let { return@withContext it }
        val file = File(File(context.filesDir, "hadith"), "$id.json")
        if (!file.exists()) return@withContext null
        runCatching {
            val parsed = AppJson.decodeFromString<HadithCollection>(file.readText())
            memory[id] = parsed
            parsed
        }.getOrNull()
    }

    suspend fun isDownloaded(id: String): Boolean = withContext(Dispatchers.IO) {
        File(File(context.filesDir, "hadith"), "$id.json").exists()
    }

    /**
     * Merges the Arabic edition with Bengali/English editions. The upstream project publishes one
     * file per language; the merge keys on hadith number and never invents missing text.
     */
    suspend fun downloadCollection(id: String, onProgress: (step: String) -> Unit = {}): Result<HadithCollection> =
        withContext(Dispatchers.IO) {
            runCatching {
                onProgress("Arabic")
                val ar = AppJson.decodeFromString<RawCollection>(fetchRaw("ara-$id"))
                onProgress("Bengali")
                val bn = runCatching { AppJson.decodeFromString<RawCollection>(fetchRaw("ben-$id")) }.getOrNull()
                onProgress("English")
                val en = runCatching { AppJson.decodeFromString<RawCollection>(fetchRaw("eng-$id")) }.getOrNull()

                val bnByNumber = bn?.hadiths?.associateBy { it.hadithnumber } ?: emptyMap()
                val enByNumber = en?.hadiths?.associateBy { it.hadithnumber } ?: emptyMap()
                val sections = ar.metadata.sections

                val merged = ar.hadiths.map { h ->
                    val number = h.hadithnumber.toIntOrNull() ?: 0
                    Hadith(
                        number = number,
                        arabic = h.text,
                        bengali = bnByNumber[h.hadithnumber]?.text ?: "",
                        english = enByNumber[h.hadithnumber]?.text ?: "",
                        grades = parseGrades(h.grades),
                        reference = HadithRef(h.reference?.book ?: 0, h.reference?.hadith ?: 0),
                        book = h.reference?.book ?: 0,
                        bookName = sections[h.reference?.book?.toString()] ?: "",
                    )
                }
                val collection = HadithCollection(
                    collection = HadithMeta(
                        id = id,
                        name = ar.metadata.name,
                        source = "https://github.com/fawazahmed0/hadith-api",
                        license = "The Unlicense (public domain)",
                        languages = listOfNotNull("ara", if (bn != null) "ben" else null, if (en != null) "eng" else null),
                        hadithCount = merged.size,
                    ),
                    sections = sections,
                    hadiths = merged,
                )
                val file = File(File(context.filesDir, "hadith").apply { mkdirs() }, "$id.json")
                file.writeText(AppJson.encodeToString(collection))
                memory[id] = collection
                collection
            }
        }

    suspend fun deleteDownloaded(id: String) = withContext(Dispatchers.IO) {
        memory.remove(id)
        File(File(context.filesDir, "hadith"), "$id.json").delete()
        Unit
    }

    suspend fun storageBytes(): Long = withContext(Dispatchers.IO) {
        File(context.filesDir, "hadith").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private suspend fun fetchRaw(path: String): String {
        val url = "$base/$path.min.json"
        val text = kotlinx.coroutines.withContext(Dispatchers.IO) {
            com.salahmate.app.core.net.withRetry {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 20_000
                connection.readTimeout = 40_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "SalahMate/1.0")
                try {
                    val stream = connection.inputStream
                    stream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }
        }
        return text
    }

    private fun parseGrades(raw: String): List<HadithGrade> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            AppJson.decodeFromString<List<HadithGrade>>(raw)
        }.getOrElse { emptyList() }
    }

    @Serializable
    private data class RawCollection(
        val metadata: RawMetadata = RawMetadata(),
        val hadiths: List<RawHadith> = emptyList(),
    )

    @Serializable
    private data class RawMetadata(
        val name: String = "",
        val sections: Map<String, String> = emptyMap(),
    )

    @Serializable
    private data class RawHadith(
        val hadithnumber: String = "",
        val text: String = "",
        val grades: String = "",
        val reference: RawReference? = null,
    )

    @Serializable
    private data class RawReference(val book: Int = 0, val hadith: Int = 0)
}
