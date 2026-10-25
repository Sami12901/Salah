package com.salahmate.app.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ---------------------------------------------------------------- Quran -----

@Entity(tableName = "quran_bookmarks", indices = [Index(value = ["surah", "ayah"], unique = true)])
data class QuranBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    val ayah: Int,
    val note: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "reading_position")
data class ReadingPositionEntity(
    @PrimaryKey val id: Int = 1,
    val surah: Int,
    val ayah: Int,
    val updatedAt: Long,
)

@Entity(tableName = "reading_history", indices = [Index(value = ["surah"], unique = true)])
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    val lastAyah: Int,
    val updatedAt: Long,
)

@Entity(tableName = "daily_reading")
data class DailyReadingEntity(
    @PrimaryKey val date: String,          // ISO yyyy-MM-dd in the user's zone
    val ayahsRead: Int,
)

// ----------------------------------------------------------------- Hifz -----

enum class HifzState { MEMORIZED, LEARNING, NEEDS_REVISION }

@Entity(tableName = "hifz_items", indices = [Index(value = ["surah", "ayahStart", "ayahEnd"], unique = true)])
data class HifzItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    val ayahStart: Int,
    val ayahEnd: Int,
    val state: String = HifzState.LEARNING.name,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long? = null,
    val reviewCount: Int = 0,
    val notes: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "hifz_review_log")
data class HifzReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val surah: Int,
    val ayahCount: Int,
    val resultingState: String,
    val reviewedAt: Long,
)

// -------------------------------------------------------------- Tracking ----

@Entity(tableName = "prayer_log", primaryKeys = ["date", "prayer"])
data class PrayerLogEntity(
    val date: String,      // ISO yyyy-MM-dd
    val prayer: String,    // FAJR / DHUHR / ASR / MAGHRIB / ISHA
    val completed: Boolean,
    val note: String? = null,
    val updatedAt: Long,
)

// ---------------------------------------------------------------- Tasbih ----

@Entity(tableName = "tasbih_sessions")
data class TasbihSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dhikrId: String,
    val target: Int,
    val count: Int,
    val startedAt: Long,
    val completedAt: Long? = null,
    val note: String? = null,
)

// ---------------------------------------------------------------- Hadith ----

@Entity(tableName = "hadith_bookmarks", indices = [Index(value = ["collection", "hadithNumber"], unique = true)])
data class HadithBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collection: String,
    val bookNumber: Int,
    val hadithNumber: Int,
    val createdAt: Long,
)

@Entity(tableName = "hadith_progress")
data class HadithProgressEntity(
    @PrimaryKey val collection: String,
    val bookNumber: Int,
    val hadithNumber: Int,
    val updatedAt: Long,
)

// ------------------------------------------------------------------- Dua ----

@Entity(tableName = "dua_favorites")
data class DuaFavoriteEntity(
    @PrimaryKey val duaId: String,
    val createdAt: Long,
)

@Entity(tableName = "dua_completions", primaryKeys = ["duaId", "date"])
data class DuaCompletionEntity(
    val duaId: String,
    val date: String,
    val completedAt: Long,
)

// ------------------------------------------------------------- Downloads ----

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val key: String,          // e.g. "translation:bn.bengali:2" or "audio:ar.alafasy:2"
    val kind: String,                     // TRANSLATION | AUDIO
    val surah: Int,
    val edition: String,
    val status: String,                   // QUEUED | RUNNING | PAUSED | COMPLETED | FAILED | CANCELLED
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String? = null,
    val updatedAt: Long,
    val error: String? = null,
)

// ----------------------------------------------------------------- Zakat ----

@Entity(tableName = "zakat_records")
data class ZakatRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val currency: String,
    val totalAssets: Double,
    val deductible: Double,
    val netWealth: Double,
    val nisabValue: Double,
    val zakatDue: Double,
    val payloadJson: String,
)

// ----------------------------------------------------------- Prayer cache ---

/**
 * Cached provider output. The cache is keyed on the exact inputs that can change a schedule, so a
 * cached row is never shown for a different location, method or madhab.
 */
@Entity(tableName = "prayer_cache", primaryKeys = ["date", "latKey", "lonKey", "method", "madhab", "provider"])
data class PrayerCacheEntity(
    val date: String,
    val latKey: String,
    val lonKey: String,
    val method: String,
    val madhab: String,
    val provider: String,
    val payloadJson: String,
    val fetchedAt: Long,
)
