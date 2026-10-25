package com.salahmate.app.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "salahmate_settings")

/**
 * Single source of truth for user preferences, backed by DataStore.
 *
 * Everything here is local to the device: no preference is ever transmitted, and there is no
 * account. Corrupt reads degrade to defaults instead of crashing the app.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val themeMode = stringPreferencesKey("theme_mode")
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val locationMode = stringPreferencesKey("location_mode")
        val lat = doublePreferencesKey("latitude")
        val lon = doublePreferencesKey("longitude")
        val locationLabel = stringPreferencesKey("location_label")
        val city = stringPreferencesKey("city")
        val country = stringPreferencesKey("country")
        val timeZone = stringPreferencesKey("time_zone")
        val method = stringPreferencesKey("calc_method")
        val madhab = stringPreferencesKey("asr_madhab")
        val highLat = stringPreferencesKey("high_lat_rule")
        val adjFajr = intPreferencesKey("adj_fajr")
        val adjSunrise = intPreferencesKey("adj_sunrise")
        val adjDhuhr = intPreferencesKey("adj_dhuhr")
        val adjAsr = intPreferencesKey("adj_asr")
        val adjMaghrib = intPreferencesKey("adj_maghrib")
        val adjIsha = intPreferencesKey("adj_isha")
        val provider = stringPreferencesKey("provider_mode")
        val translation = stringPreferencesKey("translation_edition")
        val showTranslation = booleanPreferencesKey("show_translation")
        val reciter = stringPreferencesKey("reciter_edition")
        val arabicScale = floatPreferencesKey("arabic_scale")
        val dailyAyahGoal = intPreferencesKey("daily_ayah_goal")
        val notifEnabled = booleanPreferencesKey("notif_enabled")
        val notifFajr = booleanPreferencesKey("notif_fajr")
        val notifDhuhr = booleanPreferencesKey("notif_dhuhr")
        val notifAsr = booleanPreferencesKey("notif_asr")
        val notifMaghrib = booleanPreferencesKey("notif_maghrib")
        val notifIsha = booleanPreferencesKey("notif_isha")
        val soundMode = stringPreferencesKey("sound_mode")
        val advance = intPreferencesKey("advance_minutes")
        val vibrate = booleanPreferencesKey("vibrate")
        val suhoorReminder = booleanPreferencesKey("suhoor_reminder")
        val iftarReminder = booleanPreferencesKey("iftar_reminder")
        val imsakOffset = intPreferencesKey("imsak_offset")
        val iftarOffset = intPreferencesKey("iftar_offset")
        val hijriOffset = intPreferencesKey("hijri_offset")
        val revisionGoal = intPreferencesKey("revision_goal")
        val trackingEnabled = booleanPreferencesKey("tracking_enabled")
        val nisabBasis = stringPreferencesKey("nisab_basis")
        val goldPrice = doublePreferencesKey("gold_price")
        val silverPrice = doublePreferencesKey("silver_price")
        val currency = stringPreferencesKey("currency")
        val goldUpdated = longPreferencesKey("gold_updated")
        val lastExport = longPreferencesKey("last_export")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { p ->
            val d = UserSettings()
            UserSettings(
                language = AppLanguage.from(p[Keys.language]),
                themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
                onboardingCompleted = p[Keys.onboarding] ?: d.onboardingCompleted,
                locationMode = p[Keys.locationMode]?.let { runCatching { LocationMode.valueOf(it) }.getOrNull() } ?: d.locationMode,
                latitude = p[Keys.lat] ?: d.latitude,
                longitude = p[Keys.lon] ?: d.longitude,
                locationLabel = p[Keys.locationLabel] ?: d.locationLabel,
                city = p[Keys.city] ?: d.city,
                country = p[Keys.country] ?: d.country,
                timeZoneId = p[Keys.timeZone] ?: d.timeZoneId,
                method = CalculationMethodId.from(p[Keys.method]),
                madhab = AsrMadhab.from(p[Keys.madhab]),
                highLatitudeRule = p[Keys.highLat]?.let { runCatching { HighLatitudeRuleId.valueOf(it) }.getOrNull() } ?: d.highLatitudeRule,
                adjustmentFajr = p[Keys.adjFajr] ?: 0,
                adjustmentSunrise = p[Keys.adjSunrise] ?: 0,
                adjustmentDhuhr = p[Keys.adjDhuhr] ?: 0,
                adjustmentAsr = p[Keys.adjAsr] ?: 0,
                adjustmentMaghrib = p[Keys.adjMaghrib] ?: 0,
                adjustmentIsha = p[Keys.adjIsha] ?: 0,
                providerMode = p[Keys.provider]?.let { runCatching { PrayerProviderMode.valueOf(it) }.getOrNull() } ?: d.providerMode,
                translationEdition = p[Keys.translation] ?: d.translationEdition,
                showTranslation = p[Keys.showTranslation] ?: d.showTranslation,
                reciterEdition = p[Keys.reciter] ?: d.reciterEdition,
                arabicScale = p[Keys.arabicScale] ?: d.arabicScale,
                dailyAyahGoal = p[Keys.dailyAyahGoal] ?: d.dailyAyahGoal,
                notificationsEnabled = p[Keys.notifEnabled] ?: d.notificationsEnabled,
                notifyFajr = p[Keys.notifFajr] ?: true,
                notifyDhuhr = p[Keys.notifDhuhr] ?: true,
                notifyAsr = p[Keys.notifAsr] ?: true,
                notifyMaghrib = p[Keys.notifMaghrib] ?: true,
                notifyIsha = p[Keys.notifIsha] ?: true,
                soundMode = p[Keys.soundMode]?.let { runCatching { NotificationSoundMode.valueOf(it) }.getOrNull() } ?: d.soundMode,
                advanceMinutes = p[Keys.advance] ?: d.advanceMinutes,
                vibrate = p[Keys.vibrate] ?: d.vibrate,
                suhoorReminderEnabled = p[Keys.suhoorReminder] ?: false,
                iftarReminderEnabled = p[Keys.iftarReminder] ?: false,
                imsakOffsetMinutes = p[Keys.imsakOffset] ?: d.imsakOffsetMinutes,
                iftarOffsetMinutes = p[Keys.iftarOffset] ?: d.iftarOffsetMinutes,
                hijriOffsetDays = p[Keys.hijriOffset] ?: 0,
                dailyRevisionGoal = p[Keys.revisionGoal] ?: d.dailyRevisionGoal,
                prayerTrackingEnabled = p[Keys.trackingEnabled] ?: false,
                nisabBasis = p[Keys.nisabBasis]?.let { runCatching { NisabBasis.valueOf(it) }.getOrNull() } ?: d.nisabBasis,
                goldPricePerGram = p[Keys.goldPrice] ?: 0.0,
                silverPricePerGram = p[Keys.silverPrice] ?: 0.0,
                zakatCurrency = p[Keys.currency] ?: d.zakatCurrency,
                goldPriceUpdatedAt = p[Keys.goldUpdated] ?: 0L,
                lastExportAt = p[Keys.lastExport] ?: 0L,
            )
        }
        .distinctUntilChanged()

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    suspend fun setLanguage(v: AppLanguage) = edit { it[Keys.language] = v.code }
    suspend fun setTheme(v: ThemeMode) = edit { it[Keys.themeMode] = v.name }
    suspend fun setOnboardingCompleted(v: Boolean) = edit { it[Keys.onboarding] = v }

    suspend fun setLocation(
        mode: LocationMode,
        lat: Double,
        lon: Double,
        label: String,
        timeZoneId: String,
        city: String? = null,
        country: String? = null,
    ) = edit {
        it[Keys.locationMode] = mode.name
        it[Keys.lat] = lat
        it[Keys.lon] = lon
        it[Keys.locationLabel] = label
        it[Keys.timeZone] = timeZoneId
        if (city == null) it.remove(Keys.city) else it[Keys.city] = city
        if (country == null) it.remove(Keys.country) else it[Keys.country] = country
    }

    suspend fun setMethod(v: CalculationMethodId) = edit { it[Keys.method] = v.name }
    suspend fun setMadhab(v: AsrMadhab) = edit { it[Keys.madhab] = v.name }
    suspend fun setHighLatitudeRule(v: HighLatitudeRuleId) = edit { it[Keys.highLat] = v.name }
    suspend fun setProviderMode(v: PrayerProviderMode) = edit { it[Keys.provider] = v.name }

    suspend fun setAdjustments(fajr: Int, sunrise: Int, dhuhr: Int, asr: Int, maghrib: Int, isha: Int) = edit {
        it[Keys.adjFajr] = fajr
        it[Keys.adjSunrise] = sunrise
        it[Keys.adjDhuhr] = dhuhr
        it[Keys.adjAsr] = asr
        it[Keys.adjMaghrib] = maghrib
        it[Keys.adjIsha] = isha
    }

    suspend fun setTranslationEdition(v: String) = edit { it[Keys.translation] = v }
    suspend fun setShowTranslation(v: Boolean) = edit { it[Keys.showTranslation] = v }
    suspend fun setReciter(v: String) = edit { it[Keys.reciter] = v }
    suspend fun setArabicScale(v: Float) = edit { it[Keys.arabicScale] = v.coerceIn(0.8f, 2.2f) }
    suspend fun setDailyAyahGoal(v: Int) = edit { it[Keys.dailyAyahGoal] = v.coerceIn(0, 300) }

    suspend fun setNotificationsEnabled(v: Boolean) = edit { it[Keys.notifEnabled] = v }
    suspend fun setPrayerNotification(prayer: String, enabled: Boolean) = edit {
        when (prayer) {
            "FAJR" -> it[Keys.notifFajr] = enabled
            "DHUHR" -> it[Keys.notifDhuhr] = enabled
            "ASR" -> it[Keys.notifAsr] = enabled
            "MAGHRIB" -> it[Keys.notifMaghrib] = enabled
            "ISHA" -> it[Keys.notifIsha] = enabled
        }
    }
    suspend fun setSoundMode(v: NotificationSoundMode) = edit { it[Keys.soundMode] = v.name }
    suspend fun setAdvanceMinutes(v: Int) = edit { it[Keys.advance] = v.coerceIn(0, 60) }
    suspend fun setVibrate(v: Boolean) = edit { it[Keys.vibrate] = v }

    suspend fun setSuhoorReminder(v: Boolean) = edit { it[Keys.suhoorReminder] = v }
    suspend fun setIftarReminder(v: Boolean) = edit { it[Keys.iftarReminder] = v }
    suspend fun setImsakOffset(v: Int) = edit { it[Keys.imsakOffset] = v.coerceIn(0, 30) }
    suspend fun setIftarOffset(v: Int) = edit { it[Keys.iftarOffset] = v.coerceIn(-10, 30) }

    suspend fun setHijriOffset(v: Int) = edit { it[Keys.hijriOffset] = v.coerceIn(-3, 3) }
    suspend fun setRevisionGoal(v: Int) = edit { it[Keys.revisionGoal] = v.coerceIn(1, 100) }
    suspend fun setPrayerTrackingEnabled(v: Boolean) = edit { it[Keys.trackingEnabled] = v }

    suspend fun setNisabBasis(v: NisabBasis) = edit { it[Keys.nisabBasis] = v.name }
    suspend fun setMetalPrices(gold: Double, silver: Double, updatedAt: Long) = edit {
        it[Keys.goldPrice] = gold
        it[Keys.silverPrice] = silver
        it[Keys.goldUpdated] = updatedAt
    }
    suspend fun setCurrency(v: String) = edit { it[Keys.currency] = v }
    suspend fun setLastExportAt(v: Long) = edit { it[Keys.lastExport] = v }
}
