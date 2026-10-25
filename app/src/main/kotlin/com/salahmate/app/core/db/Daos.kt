package com.salahmate.app.core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAt DESC")
    fun bookmarks(): Flow<List<QuranBookmarkEntity>>

    @Query("SELECT * FROM quran_bookmarks WHERE surah = :surah AND ayah = :ayah LIMIT 1")
    suspend fun bookmarkAt(surah: Int, ayah: Int): QuranBookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: QuranBookmarkEntity): Long

    @Query("DELETE FROM quran_bookmarks WHERE surah = :surah AND ayah = :ayah")
    suspend fun deleteBookmark(surah: Int, ayah: Int)

    @Query("UPDATE quran_bookmarks SET note = :note WHERE surah = :surah AND ayah = :ayah")
    suspend fun updateBookmarkNote(surah: Int, ayah: Int, note: String?)

    @Query("SELECT * FROM reading_position WHERE id = 1")
    fun readingPosition(): Flow<ReadingPositionEntity?>

    @Query("SELECT * FROM reading_position WHERE id = 1")
    suspend fun readingPositionOnce(): ReadingPositionEntity?

    @Upsert
    suspend fun saveReadingPosition(position: ReadingPositionEntity)

    @Query("SELECT * FROM reading_history ORDER BY updatedAt DESC")
    fun readingHistory(): Flow<List<ReadingHistoryEntity>>

    @Query("UPDATE reading_history SET lastAyah = :ayah, updatedAt = :updatedAt WHERE surah = :surah")
    suspend fun updateHistory(surah: Int, ayah: Int, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ReadingHistoryEntity): Long

    @Query("SELECT * FROM daily_reading WHERE date = :date")
    suspend fun dailyReading(date: String): DailyReadingEntity?

    @Query("SELECT * FROM daily_reading ORDER BY date DESC LIMIT :limit")
    fun recentDailyReading(limit: Int = 30): Flow<List<DailyReadingEntity>>

    @Upsert
    suspend fun upsertDailyReading(reading: DailyReadingEntity)
}

@Dao
interface HifzDao {
    @Query("SELECT * FROM hifz_items ORDER BY surah, ayahStart")
    fun items(): Flow<List<HifzItemEntity>>

    @Query("SELECT * FROM hifz_items WHERE state = :state ORDER BY surah, ayahStart")
    fun itemsByState(state: String): Flow<List<HifzItemEntity>>

    @Query("SELECT * FROM hifz_items WHERE surah = :surah AND ayahStart = :start AND ayahEnd = :end LIMIT 1")
    suspend fun find(surah: Int, start: Int, end: Int): HifzItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: HifzItemEntity): Long

    @Query("UPDATE hifz_items SET state = :state, notes = :notes WHERE id = :id")
    suspend fun updateItem(id: Long, state: String, notes: String?)

    @Query("UPDATE hifz_items SET state = :state, lastReviewedAt = :reviewedAt, nextReviewAt = :nextReviewAt, reviewCount = reviewCount + 1 WHERE id = :id")
    suspend fun updateReview(id: Long, state: String, reviewedAt: Long, nextReviewAt: Long)

    @Query("UPDATE hifz_items SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    @Query("DELETE FROM hifz_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun logReview(log: HifzReviewLogEntity)

    @Query("SELECT * FROM hifz_review_log ORDER BY reviewedAt DESC LIMIT :limit")
    fun reviewLog(limit: Int = 120): Flow<List<HifzReviewLogEntity>>

    @Query("SELECT COUNT(*) FROM hifz_review_log WHERE date = :date")
    fun reviewsOn(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(ayahCount), 0) FROM hifz_review_log WHERE date = :date")
    fun ayahsReviewedOn(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM hifz_items WHERE state = :state")
    fun countByState(state: String): Flow<Int>
}

@Dao
interface PrayerTrackingDao {
    @Query("SELECT * FROM prayer_log WHERE date = :date")
    fun forDate(date: String): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_log WHERE date BETWEEN :from AND :to ORDER BY date")
    fun between(from: String, to: String): Flow<List<PrayerLogEntity>>

    @Upsert
    suspend fun upsert(log: PrayerLogEntity)

    @Query("DELETE FROM prayer_log")
    suspend fun clearAll()
}

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_sessions ORDER BY startedAt DESC")
    fun sessions(): Flow<List<TasbihSessionEntity>>

    @Insert
    suspend fun insert(session: TasbihSessionEntity): Long

    @Query("UPDATE tasbih_sessions SET count = :count WHERE id = :id")
    suspend fun updateCount(id: Long, count: Int)

    @Query("UPDATE tasbih_sessions SET completedAt = :completedAt, note = :note WHERE id = :id")
    suspend fun complete(id: Long, completedAt: Long?, note: String?)

    @Delete
    suspend fun delete(session: TasbihSessionEntity)

    @Query("SELECT * FROM tasbih_sessions WHERE completedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): TasbihSessionEntity?

    @Query("SELECT COALESCE(SUM(count), 0) FROM tasbih_sessions WHERE startedAt >= :since")
    fun totalSince(since: Long): Flow<Int>
}

@Dao
interface HadithDao {
    @Query("SELECT * FROM hadith_bookmarks ORDER BY createdAt DESC")
    fun bookmarks(): Flow<List<HadithBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun bookmark(bookmark: HadithBookmarkEntity)

    @Query("DELETE FROM hadith_bookmarks WHERE collection = :collection AND hadithNumber = :number")
    suspend fun unbookmark(collection: String, number: Int)

    @Query("SELECT * FROM hadith_progress WHERE collection = :collection")
    fun progress(collection: String): Flow<HadithProgressEntity?>

    @Upsert
    suspend fun saveProgress(progress: HadithProgressEntity)
}

@Dao
interface DuaDao {
    @Query("SELECT * FROM dua_favorites")
    fun favorites(): Flow<List<DuaFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun favorite(favorite: DuaFavoriteEntity)

    @Query("DELETE FROM dua_favorites WHERE duaId = :duaId")
    suspend fun unfavorite(duaId: String)

    @Query("SELECT * FROM dua_completions WHERE date = :date")
    fun completionsOn(date: String): Flow<List<DuaCompletionEntity>>

    @Upsert
    suspend fun complete(completion: DuaCompletionEntity)

    @Query("DELETE FROM dua_completions WHERE duaId = :duaId AND date = :date")
    suspend fun uncomplete(duaId: String, date: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun all(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE key = :key")
    suspend fun byKey(key: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE surah = :surah AND edition = :edition AND kind = :kind LIMIT 1")
    suspend fun find(kind: String, surah: Int, edition: String): DownloadEntity?

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE key = :key")
    suspend fun delete(key: String)

    @Query("SELECT COALESCE(SUM(downloadedBytes), 0) FROM downloads")
    fun totalBytes(): Flow<Long>
}

@Dao
interface ZakatDao {
    @Query("SELECT * FROM zakat_records ORDER BY createdAt DESC")
    fun records(): Flow<List<ZakatRecordEntity>>

    @Insert
    suspend fun insert(record: ZakatRecordEntity): Long

    @Query("DELETE FROM zakat_records WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PrayerCacheDao {
    @Query("SELECT * FROM prayer_cache WHERE date = :date AND latKey = :latKey AND lonKey = :lonKey AND method = :method AND madhab = :madhab AND provider = :provider LIMIT 1")
    suspend fun get(date: String, latKey: String, lonKey: String, method: String, madhab: String, provider: String): PrayerCacheEntity?

    @Upsert
    suspend fun put(entity: PrayerCacheEntity)

    @Query("SELECT * FROM prayer_cache WHERE date = :date ORDER BY fetchedAt DESC")
    suspend fun forDate(date: String): List<PrayerCacheEntity>

    @Query("DELETE FROM prayer_cache WHERE fetchedAt < :olderThan")
    suspend fun prune(olderThan: Long): Int
}
