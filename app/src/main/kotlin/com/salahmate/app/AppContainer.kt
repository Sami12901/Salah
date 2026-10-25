package com.salahmate.app

import android.content.Context
import com.salahmate.app.core.data.BackupManager
import com.salahmate.app.core.db.SalahMateDatabase
import com.salahmate.app.core.location.LocationProvider
import com.salahmate.app.core.net.HttpClients
import com.salahmate.app.core.prefs.SettingsRepository
import com.salahmate.app.core.util.ClockProvider
import com.salahmate.app.core.util.SystemClockProvider
import com.salahmate.app.data.prayer.AlAdhanApi
import com.salahmate.app.data.prayer.AlAdhanProvider
import com.salahmate.app.data.prayer.LocalAdhanProvider
import com.salahmate.app.data.prayer.PrayerTimesRepository
import com.salahmate.app.data.quran.AlQuranCloudApi
import com.salahmate.app.data.hadith.HadithRepository
import com.salahmate.app.data.quran.QuranRepository

/**
 * Explicit dependency container (constructor injection without a DI framework).
 *
 * A plain object graph keeps the app understandable and testable: every repository takes its
 * dependencies as constructor parameters, so tests construct the pieces they need with fakes.
 * If the project later grows an optional self-hosted backend, only this file changes for wiring.
 */
class AppContainer(
    context: Context,
    val clockProvider: ClockProvider = SystemClockProvider,
) {
    /** Application context, exposed for components that need it (widgets, workers, audio). */
    val appContext: android.content.Context = context.applicationContext

    val database: SalahMateDatabase by lazy { SalahMateDatabase.build(appContext) }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }
    val locationProvider: LocationProvider by lazy { LocationProvider(appContext) }

    private val okHttp by lazy { HttpClients.okHttp(enableLogging = BuildConfig.DEBUG) }

    private val alAdhanApi: AlAdhanApi by lazy {
        HttpClients.retrofit("https://api.aladhan.com/", okHttp).create(AlAdhanApi::class.java)
    }

    private val quranApi: AlQuranCloudApi by lazy {
        HttpClients.retrofit("https://api.alquran.cloud/", okHttp).create(AlQuranCloudApi::class.java)
    }

    val prayerTimes: PrayerTimesRepository by lazy {
        PrayerTimesRepository(
            localProvider = LocalAdhanProvider(),
            onlineProvider = AlAdhanProvider(alAdhanApi),
            cacheDao = database.prayerCacheDao(),
        )
    }

    val quran: QuranRepository by lazy {
        QuranRepository(appContext, database.quranDao(), quranApi)
    }

    val hadith: HadithRepository by lazy { HadithRepository(appContext) }

    val backupManager: BackupManager by lazy {
        BackupManager(appContext, database, BuildConfig.VERSION_NAME)
    }
}
