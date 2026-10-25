package com.salahmate.app.core.data

import android.content.Context
import android.net.Uri
import com.salahmate.app.core.db.SalahMateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * What a backup contains. Deliberately limited to data the user *owns* in the app: bookmarks,
 * reading position, hifz progress, prayer tracking, tasbih sessions, dua favourites, hadith
 * progress and note fields. Preferences, download caches and anything about the device are not
 * included.
 */
@Serializable
data class BackupPayload(
    val formatVersion: Int = 1,
    val createdAt: Long = 0,
    val appVersion: String = "",
    val bookmarks: List<BackupBookmark> = emptyList(),
    val readingPosition: BackupReadingPosition? = null,
    val readingHistory: List<BackupHistory> = emptyList(),
    val hifz: List<BackupHifz> = emptyList(),
    val prayerLog: List<BackupPrayerLog> = emptyList(),
    val tasbih: List<BackupTasbih> = emptyList(),
    val duaFavorites: List<String> = emptyList(),
    val hadithBookmarks: List<BackupHadithBookmark> = emptyList(),
)

@Serializable data class BackupBookmark(val surah: Int, val ayah: Int, val note: String?, val createdAt: Long)
@Serializable data class BackupReadingPosition(val surah: Int, val ayah: Int, val updatedAt: Long)
@Serializable data class BackupHistory(val surah: Int, val lastAyah: Int, val updatedAt: Long)
@Serializable data class BackupHifz(
    val surah: Int, val ayahStart: Int, val ayahEnd: Int, val state: String,
    val lastReviewedAt: Long?, val nextReviewAt: Long?, val reviewCount: Int, val notes: String?,
)
@Serializable data class BackupPrayerLog(val date: String, val prayer: String, val completed: Boolean, val note: String?)
@Serializable data class BackupTasbih(
    val dhikrId: String, val target: Int, val count: Int, val startedAt: Long, val completedAt: Long?, val note: String?,
)
@Serializable data class BackupHadithBookmark(val collection: String, val bookNumber: Int, val hadithNumber: Int, val createdAt: Long)

sealed interface BackupResult {
    data class Success(val bytesWritten: Int, val encrypted: Boolean) : BackupResult
    data class Failure(val message: String) : BackupResult
}

/**
 * Export/import of user-owned data.
 *
 * Privacy: an export contains personal religious records, so the screen offers (and recommends) a
 * password. With a password the payload is encrypted with AES-256-GCM using a key derived by
 * PBKDF2-HMAC-SHA256 (60,000 iterations, random 16-byte salt, random 12-byte IV). Without a
 * password the JSON is written in the clear and the UI says so explicitly.
 */
class BackupManager(
    private val context: Context,
    private val database: SalahMateDatabase,
    private val appVersion: String,
) {

    suspend fun export(uri: Uri, password: String?): BackupResult = withContext(Dispatchers.IO) {
        try {
            val payload = collect()
            val json = kotlinx.serialization.json.Json { prettyPrint = false; encodeDefaults = true }
                .encodeToString(BackupPayload.serializer(), payload)
            val bytes = if (password.isNullOrBlank()) {
                json.toByteArray(Charsets.UTF_8)
            } else {
                Crypto.encrypt(json.toByteArray(Charsets.UTF_8), password)
            }
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                ?: return@withContext BackupResult.Failure("Could not open the selected file")
            BackupResult.Success(bytes.size, encrypted = !password.isNullOrBlank())
        } catch (t: Throwable) {
            BackupResult.Failure(t.message ?: t::class.java.simpleName)
        }
    }

    suspend fun import(uri: Uri, password: String?): BackupResult = withContext(Dispatchers.IO) {
        try {
            val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext BackupResult.Failure("Could not read the selected file")
            val isEncrypted = Crypto.looksEncrypted(raw)
            if (isEncrypted && password.isNullOrBlank()) {
                return@withContext BackupResult.Failure("This backup is encrypted. Enter its password to import it.")
            }
            val jsonBytes = if (isEncrypted) {
                runCatching { Crypto.decrypt(raw, password.orEmpty()) }
                    .getOrElse { return@withContext BackupResult.Failure("Wrong password, or the file is damaged.") }
            } else {
                raw
            }
            val text = jsonBytes.toString(Charsets.UTF_8)
            val payload = runCatching {
                kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                    .decodeFromString(BackupPayload.serializer(), text)
            }.getOrElse { return@withContext BackupResult.Failure("Not a SalahMate backup file.") }
            if (payload.formatVersion > 1) {
                return@withContext BackupResult.Failure("This backup was written by a newer app version.")
            }
            restore(payload)
            BackupResult.Success(jsonBytes.size, encrypted = isEncrypted)
        } catch (t: Throwable) {
            BackupResult.Failure(t.message ?: t::class.java.simpleName)
        }
    }

    private suspend fun collect(): BackupPayload {
        val quranDao = database.quranDao()
        val db = database
        // Flows are collected once here; a backup is a point-in-time snapshot.
        val bookmarks = quranDao.bookmarks().first().map {
            BackupBookmark(it.surah, it.ayah, it.note, it.createdAt)
        }
        val position = quranDao.readingPosition().first()?.let {
            BackupReadingPosition(it.surah, it.ayah, it.updatedAt)
        }
        val history = quranDao.readingHistory().first().map {
            BackupHistory(it.surah, it.lastAyah, it.updatedAt)
        }
        val hifz = db.hifzDao().items().first().map {
            BackupHifz(it.surah, it.ayahStart, it.ayahEnd, it.state, it.lastReviewedAt, it.nextReviewAt, it.reviewCount, it.notes)
        }
        val tasbih = db.tasbihDao().sessions().first().map {
            BackupTasbih(it.dhikrId, it.target, it.count, it.startedAt, it.completedAt, it.note)
        }
        val duas = db.duaDao().favorites().first().map { it.duaId }
        val hadith = db.hadithDao().bookmarks().first().map {
            BackupHadithBookmark(it.collection, it.bookNumber, it.hadithNumber, it.createdAt)
        }
        val prayerLog = mutableListOf<BackupPrayerLog>()
        // Prayer log is read across a wide window; entries outside it are not exported.
        val from = java.time.LocalDate.now().minusYears(2).toString()
        val to = java.time.LocalDate.now().plusYears(2).toString()
        db.prayerTrackingDao().between(from, to).first().forEach {
            prayerLog.add(BackupPrayerLog(it.date, it.prayer, it.completed, it.note))
        }

        return BackupPayload(
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion,
            bookmarks = bookmarks,
            readingPosition = position,
            readingHistory = history,
            hifz = hifz,
            prayerLog = prayerLog,
            tasbih = tasbih,
            duaFavorites = duas,
            hadithBookmarks = hadith,
        )
    }

    /**
     * Import is additive and idempotent: records are upserted by their natural keys, so importing
     * the same file twice does not duplicate data. Existing newer progress is not overwritten.
     */
    private suspend fun restore(payload: BackupPayload) {
        val quranDao = database.quranDao()
        payload.bookmarks.forEach { b ->
            quranDao.insertBookmark(
                com.salahmate.app.core.db.QuranBookmarkEntity(
                    surah = b.surah, ayah = b.ayah, note = b.note, createdAt = b.createdAt,
                ),
            )
        }
        payload.readingPosition?.let { pos ->
            val existing = quranDao.readingPositionOnce()
            if (existing == null || existing.updatedAt < pos.updatedAt) {
                quranDao.saveReadingPosition(
                    com.salahmate.app.core.db.ReadingPositionEntity(id = 1, surah = pos.surah, ayah = pos.ayah, updatedAt = pos.updatedAt),
                )
            }
        }
        payload.readingHistory.forEach { h ->
            val updated = quranDao.updateHistory(h.surah, h.lastAyah, h.updatedAt)
            if (updated == 0) {
                quranDao.insertHistory(
                    com.salahmate.app.core.db.ReadingHistoryEntity(surah = h.surah, lastAyah = h.lastAyah, updatedAt = h.updatedAt),
                )
            }
        }
        payload.hifz.forEach { h ->
            val existing = database.hifzDao().find(h.surah, h.ayahStart, h.ayahEnd)
            if (existing == null) {
                database.hifzDao().insert(
                    com.salahmate.app.core.db.HifzItemEntity(
                        surah = h.surah, ayahStart = h.ayahStart, ayahEnd = h.ayahEnd, state = h.state,
                        lastReviewedAt = h.lastReviewedAt, nextReviewAt = h.nextReviewAt,
                        reviewCount = h.reviewCount, notes = h.notes, createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        payload.prayerLog.forEach { log ->
            database.prayerTrackingDao().upsert(
                com.salahmate.app.core.db.PrayerLogEntity(
                    date = log.date, prayer = log.prayer, completed = log.completed,
                    note = log.note, updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        payload.tasbih.forEach { t ->
            database.tasbihDao().insert(
                com.salahmate.app.core.db.TasbihSessionEntity(
                    dhikrId = t.dhikrId, target = t.target, count = t.count,
                    startedAt = t.startedAt, completedAt = t.completedAt, note = t.note,
                ),
            )
        }
        payload.duaFavorites.forEach { id ->
            database.duaDao().favorite(
                com.salahmate.app.core.db.DuaFavoriteEntity(duaId = id, createdAt = System.currentTimeMillis()),
            )
        }
        payload.hadithBookmarks.forEach { b ->
            database.hadithDao().bookmark(
                com.salahmate.app.core.db.HadithBookmarkEntity(
                    collection = b.collection, bookNumber = b.bookNumber,
                    hadithNumber = b.hadithNumber, createdAt = b.createdAt,
                ),
            )
        }
    }
}

/** AES-256-GCM with a PBKDF2-derived key. Format: "SMBK1" | salt(16) | iv(12) | ciphertext+tag. */
internal object Crypto {
    private const val MAGIC = "SMBK1"
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun looksEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.length + SALT_BYTES + IV_BYTES &&
            String(bytes, 0, MAGIC.length, Charsets.US_ASCII) == MAGIC

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain)
        return MAGIC.toByteArray(Charsets.US_ASCII) + salt + iv + encrypted
    }

    fun decrypt(payload: ByteArray, password: String): ByteArray {
        require(looksEncrypted(payload)) { "not a SalahMate encrypted backup" }
        val salt = payload.copyOfRange(MAGIC.length, MAGIC.length + SALT_BYTES)
        val iv = payload.copyOfRange(MAGIC.length + SALT_BYTES, MAGIC.length + SALT_BYTES + IV_BYTES)
        val cipherText = payload.copyOfRange(MAGIC.length + SALT_BYTES + IV_BYTES, payload.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }
}
