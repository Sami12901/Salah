package com.salahmate.app.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        QuranBookmarkEntity::class,
        ReadingPositionEntity::class,
        ReadingHistoryEntity::class,
        DailyReadingEntity::class,
        HifzItemEntity::class,
        HifzReviewLogEntity::class,
        PrayerLogEntity::class,
        TasbihSessionEntity::class,
        HadithBookmarkEntity::class,
        HadithProgressEntity::class,
        DuaFavoriteEntity::class,
        DuaCompletionEntity::class,
        DownloadEntity::class,
        ZakatRecordEntity::class,
        PrayerCacheEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SalahMateDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun hifzDao(): HifzDao
    abstract fun prayerTrackingDao(): PrayerTrackingDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun hadithDao(): HadithDao
    abstract fun duaDao(): DuaDao
    abstract fun downloadDao(): DownloadDao
    abstract fun zakatDao(): ZakatDao
    abstract fun prayerCacheDao(): PrayerCacheDao

    companion object {
        const val NAME = "salahmate.db"

        /**
         * v1 -> v2: added an explicit `error` column to the download table so a failed download can
         * explain itself instead of silently disappearing. Additive, so no data loss.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN error TEXT")
            }
        }

        fun build(context: Context): SalahMateDatabase =
            Room.databaseBuilder(context, SalahMateDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
