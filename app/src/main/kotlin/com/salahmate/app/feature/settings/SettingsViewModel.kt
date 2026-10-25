package com.salahmate.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salahmate.app.AppContainer
import com.salahmate.app.core.data.BackupManager
import com.salahmate.app.core.data.BackupResult
import com.salahmate.app.core.location.City
import com.salahmate.app.core.location.Cities
import com.salahmate.app.core.location.LocationResult
import com.salahmate.app.core.notify.NotificationPermission
import com.salahmate.app.core.notify.PrayerAlarmScheduler
import com.salahmate.app.core.notify.PrayerNotifier
import com.salahmate.app.core.notify.PrayerScheduling
import com.salahmate.app.core.notify.ScheduleReport
import com.salahmate.app.core.prefs.AsrMadhab
import com.salahmate.app.core.prefs.CalculationMethodId
import com.salahmate.app.core.prefs.HighLatitudeRuleId
import com.salahmate.app.core.prefs.LocationMode
import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.core.prefs.PrayerProviderMode
import com.salahmate.app.core.prefs.ThemeMode
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.prefs.AppLanguage
import com.salahmate.app.data.quran.QuranEditions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class SettingsUiState(
    val storageBytes: Long = 0,
    val hadithStorageBytes: Long = 0,
    val message: String? = null,
    val busy: Boolean = false,
    val notificationsGranted: Boolean = false,
    val exactAlarmsAvailable: Boolean = true,
    val scheduleReport: ScheduleReport? = null,
    val locationBusy: Boolean = false,
)

class SettingsViewModel(
    private val container: AppContainer,
    private val appContext: android.content.Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui

    val settings: StateFlow<UserSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val scheduler = PrayerAlarmScheduler(appContext)
            _ui.value = _ui.value.copy(
                notificationsGranted = NotificationPermission.isGranted(appContext),
                exactAlarmsAvailable = scheduler.canScheduleExact(),
                storageBytes = runCatching { container.quran.storageUsageBytes() }.getOrDefault(0L),
                hadithStorageBytes = runCatching { container.hadith.storageBytes() }.getOrDefault(0L),
            )
        }
    }

    // ------------------------------------------------------------- general ----

    fun setLanguage(language: AppLanguage) = viewModelScope.launch { container.settings.setLanguage(language) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { container.settings.setTheme(mode) }
    fun setHijriOffset(days: Int) = viewModelScope.launch { container.settings.setHijriOffset(days) }

    // ------------------------------------------------------------ location ----

    fun selectCity(city: City) = viewModelScope.launch {
        container.settings.setLocation(
            mode = LocationMode.MANUAL,
            lat = city.latitude,
            lon = city.longitude,
            label = city.label,
            timeZoneId = city.timeZoneId,
            city = city.nameEn,
            country = city.countryEn,
        )
        onScheduleAffectingChange()
        _ui.value = _ui.value.copy(message = "Location set to ${city.label}")
    }

    fun setManualCoordinates(lat: Double, lon: Double, label: String) = viewModelScope.launch {
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            _ui.value = _ui.value.copy(message = "Coordinates must be within -90..90 and -180..180")
            return@launch
        }
        val nearest = Cities.nearest(lat, lon)
        container.settings.setLocation(
            mode = LocationMode.MANUAL,
            lat = lat,
            lon = lon,
            label = label.ifBlank { "%.4f, %.4f".format(lat, lon) },
            timeZoneId = nearest.timeZoneId,
            city = null,
            country = null,
        )
        onScheduleAffectingChange()
        _ui.value = _ui.value.copy(message = "Custom coordinates saved")
    }

    fun useDeviceLocation() = viewModelScope.launch {
        _ui.value = _ui.value.copy(locationBusy = true)
        try {
            when (val result = container.locationProvider.currentLocation()) {
                is LocationResult.Success -> {
                    val loc = result.location
                    val nearest = Cities.nearest(loc.latitude, loc.longitude)
                    container.settings.setLocation(
                        mode = LocationMode.GPS,
                        lat = loc.latitude,
                        lon = loc.longitude,
                        label = "%.4f, %.4f".format(loc.latitude, loc.longitude),
                        timeZoneId = ZoneId.systemDefault().id,
                        city = null,
                        country = null,
                    )
                    onScheduleAffectingChange()
                    _ui.value = _ui.value.copy(message = "Using device location (nearest known city: ${nearest.nameEn})")
                }
                LocationResult.PermissionDenied -> _ui.value = _ui.value.copy(
                    message = "Location permission denied. Pick a city instead — it works exactly the same.",
                )
                LocationResult.ProviderDisabled -> _ui.value = _ui.value.copy(
                    message = "Location services are switched off on this device.",
                )
                LocationResult.Timeout -> _ui.value = _ui.value.copy(
                    message = "Could not get a location fix in time. Pick a city instead.",
                )
                LocationResult.Unavailable -> _ui.value = _ui.value.copy(message = "Location is unavailable on this device.")
            }
        } finally {
            _ui.value = _ui.value.copy(locationBusy = false)
        }
    }

    // --------------------------------------------------------- calculation ----

    fun setMethod(method: CalculationMethodId) = viewModelScope.launch {
        container.settings.setMethod(method)
        onScheduleAffectingChange()
        _ui.value = _ui.value.copy(message = "Calculation method: ${method.labelEn}")
    }

    fun setMadhab(madhab: AsrMadhab) = viewModelScope.launch {
        container.settings.setMadhab(madhab)
        onScheduleAffectingChange()
        _ui.value = _ui.value.copy(message = "Asr school: ${madhab.labelEn}")
    }

    fun setHighLatitudeRule(rule: HighLatitudeRuleId) = viewModelScope.launch {
        container.settings.setHighLatitudeRule(rule)
        onScheduleAffectingChange()
    }

    fun setProviderMode(mode: PrayerProviderMode) = viewModelScope.launch {
        container.settings.setProviderMode(mode)
        onScheduleAffectingChange()
    }

    fun setAdjustments(
        fajr: Int, sunrise: Int, dhuhr: Int, asr: Int, maghrib: Int, isha: Int,
    ) = viewModelScope.launch {
        container.settings.setAdjustments(fajr, sunrise, dhuhr, asr, maghrib, isha)
        onScheduleAffectingChange()
    }

    // ------------------------------------------------------- notifications ----

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled && !NotificationPermission.isGranted(appContext)) {
            _ui.value = _ui.value.copy(message = "Grant notification permission first.")
            return@launch
        }
        container.settings.setNotificationsEnabled(enabled)
        reschedule()
    }

    fun setPrayerNotification(prayer: String, enabled: Boolean) = viewModelScope.launch {
        container.settings.setPrayerNotification(prayer, enabled)
        reschedule()
    }

    fun setSoundMode(mode: NotificationSoundMode) = viewModelScope.launch {
        container.settings.setSoundMode(mode)
        reschedule()
    }

    fun setAdvanceMinutes(minutes: Int) = viewModelScope.launch {
        container.settings.setAdvanceMinutes(minutes)
        reschedule()
    }

    fun setVibrate(enabled: Boolean) = viewModelScope.launch { container.settings.setVibrate(enabled) }

    fun sendTestNotification() = viewModelScope.launch {
        if (!NotificationPermission.isGranted(appContext)) {
            _ui.value = _ui.value.copy(message = "Notification permission is not granted.")
            return@launch
        }
        PrayerNotifier.showTestNotification(appContext, settings.value.soundMode)
        _ui.value = _ui.value.copy(message = "Test notification sent.")
    }

    fun rescheduleNow() = viewModelScope.launch {
        reschedule()
        _ui.value = _ui.value.copy(message = "Reminders re-scheduled.")
    }

    private suspend fun reschedule() {
        val current = container.settings.settings.first()
        val report = PrayerScheduling.ensureScheduled(appContext, container, current)
        _ui.value = _ui.value.copy(scheduleReport = report)
    }

    // --------------------------------------------------------------- quran ----

    fun setTranslationEdition(id: String) = viewModelScope.launch { container.settings.setTranslationEdition(id) }
    fun setShowTranslation(show: Boolean) = viewModelScope.launch { container.settings.setShowTranslation(show) }
    fun setReciter(id: String) = viewModelScope.launch { container.settings.setReciter(id) }
    fun setArabicScale(scale: Float) = viewModelScope.launch { container.settings.setArabicScale(scale) }
    fun setDailyAyahGoal(goal: Int) = viewModelScope.launch { container.settings.setDailyAyahGoal(goal) }

    // ---------------------------------------------------------------- data ----

    fun exportData(uri: android.net.Uri, password: String?) = viewModelScope.launch {
        _ui.value = _ui.value.copy(busy = true)
        val result = container.backupManager.export(uri, password)
        _ui.value = _ui.value.copy(
            busy = false,
            message = when (result) {
                is BackupResult.Success -> "Backup written (${result.bytesWritten / 1024} KB, " +
                    if (result.encrypted) "encrypted)" else "not encrypted)"
                is BackupResult.Failure -> "Backup failed: ${result.message}"
            },
        )
        if (result is BackupResult.Success) container.settings.setLastExportAt(System.currentTimeMillis())
    }

    fun importData(uri: android.net.Uri, password: String?) = viewModelScope.launch {
        _ui.value = _ui.value.copy(busy = true)
        val result = container.backupManager.import(uri, password)
        _ui.value = _ui.value.copy(
            busy = false,
            message = when (result) {
                is BackupResult.Success -> "Backup restored (${result.bytesWritten / 1024} KB)."
                is BackupResult.Failure -> "Import failed: ${result.message}"
            },
        )
        refreshStatus()
    }

    fun clearQuranCache() = viewModelScope.launch {
        container.quran.deleteCachedDownloads()
        _ui.value = _ui.value.copy(message = "Downloaded translations and audio removed.")
        refreshStatus()
    }

    fun clearHadithDownloads() = viewModelScope.launch {
        val manifest = container.hadith.manifest()
        manifest.downloadable.forEach { container.hadith.deleteDownloaded(it.id) }
        _ui.value = _ui.value.copy(message = "Downloaded hadith collections removed.")
        refreshStatus()
    }

    fun clearMessage() { _ui.value = _ui.value.copy(message = null) }

    private suspend fun onScheduleAffectingChange() {
        val current = container.settings.settings.first()
        val report = PrayerScheduling.ensureScheduled(appContext, container, current)
        _ui.value = _ui.value.copy(scheduleReport = report)
    }

    fun todayForDisplay(): LocalDate = LocalDate.now(
        runCatching { ZoneId.of(settings.value.timeZoneId) }.getOrDefault(ZoneId.systemDefault()),
    )

    val translations = QuranEditions.all
    val reciters = QuranEditions.RECITERS
}
