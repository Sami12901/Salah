package com.salahmate.app.feature.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salahmate.app.AppContainer
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.PrayerMonth
import com.salahmate.app.domain.prayer.TimetableRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TimetableUiState(
    val loading: Boolean = true,
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val rows: List<TimetableRow> = emptyList(),
    val missing: List<LocalDate> = emptyList(),
    val methodLabel: String = "",
    val madhabLabel: String = "",
    val zoneLabel: String = "",
    val error: String? = null,
)

/**
 * Monthly prayer timetable.
 *
 * Uses the same repository and the same calculation settings as the home screen, so the timetable can
 * never disagree with the countdown. Days are computed on device (network access is not required for
 * the local provider) and partial days are reported rather than filled with invented times.
 */
class TimetableViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(TimetableUiState())
    val ui: StateFlow<TimetableUiState> = _ui

    private var loadedKey: String? = null

    fun load(settings: UserSettings, force: Boolean = false) {
        val today = LocalDate.now()
        loadMonth(settings, _ui.value.year, _ui.value.month, force)
        if (_ui.value.rows.isEmpty() && !force) {
            _ui.value = _ui.value.copy(year = today.year, month = today.monthValue)
        }
    }

    fun loadMonth(settings: UserSettings, year: Int, month: Int, force: Boolean = false) {
        val key = "$year-$month-${settings.latitude}-${settings.longitude}-${settings.method}-${settings.madhab}-${settings.providerMode}-${settings.timeZoneId}"
        if (!force && key == loadedKey && _ui.value.rows.isNotEmpty()) return
        loadedKey = key
        _ui.value = _ui.value.copy(loading = true, year = year, month = month, error = null)
        viewModelScope.launch {
            val zone = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
            val days = runCatching { container.prayerTimes.monthSchedule(settings, year, month) }
                .getOrDefault(emptyList())
            val rows = PrayerMonth.build(days, zone, settings.hijriOffsetDays, formatter)
            val missing = PrayerMonth.missingDates(rows, year, month)
            _ui.value = _ui.value.copy(
                loading = false,
                year = year,
                month = month,
                rows = rows,
                missing = missing,
                methodLabel = settings.method.labelEn,
                madhabLabel = settings.madhab.labelEn,
                zoneLabel = settings.timeZoneId,
                error = if (rows.isEmpty()) {
                    "No schedule could be calculated for this month. Check the location and provider settings."
                } else null,
            )
        }
    }

    fun shiftMonth(settings: UserSettings, offset: Int) {
        val (year, month) = PrayerMonth.shift(_ui.value.year, _ui.value.month, offset)
        loadMonth(settings, year, month)
    }

    fun goToCurrentMonth(settings: UserSettings) {
        val today = LocalDate.now()
        loadMonth(settings, today.year, today.monthValue)
    }
}
