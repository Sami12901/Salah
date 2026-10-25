package com.salahmate.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salahmate.app.AppContainer
import com.salahmate.app.core.db.ReadingPositionEntity
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.util.ClockProvider
import com.salahmate.app.core.util.HijriCalendar
import com.salahmate.app.core.util.HijriDate
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerProgress
import com.salahmate.app.domain.prayer.PrayerSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val loading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val day: DayPrayerTimes? = null,
    val progress: PrayerProgress? = null,
    val today: LocalDate = LocalDate.now(),
    val hijri: HijriDate? = null,
    val fromCache: Boolean = false,
    val fellBackToLocal: Boolean = false,
    val errorMessage: String? = null,
    val lastReadSurah: Int? = null,
    val lastReadAyah: Int? = null,
    val ayahsToday: Int = 0,
    val dailyGoal: Int = 10,
    val zone: ZoneId = ZoneId.systemDefault(),
)

/**
 * Home screen state.
 *
 * Recomputation happens when the date rolls over or settings change; the per-second countdown
 * lives in the UI layer and re-renders only its own text node, never the whole screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val container: AppContainer,
    private val clockProvider: ClockProvider = container.clockProvider,
) : ViewModel() {

    private val dateFlow = MutableStateFlow(LocalDate.now(clockProvider.clock))

    /** Called when the screen resumes so a rollover past midnight is picked up. */
    fun refreshDate() {
        val today = LocalDate.now(clockProvider.clock)
        if (today != dateFlow.value) dateFlow.value = today
    }

    val state: StateFlow<HomeUiState> = combine(container.settings.settings, dateFlow) { settings, date ->
        settings to date
    }.flatMapLatest { (settings, date) ->
        flow {
            emit(load(settings, date))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private suspend fun load(settings: UserSettings, date: LocalDate): HomeUiState {
        val zone = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val dayState = container.prayerTimes.loadDay(settings, date)
        val (yesterday, tomorrow) = container.prayerTimes.loadNeighbours(settings, date)
        val progress = if (dayState.day.hasAllObligatory) {
            PrayerSchedule.progress(dayState.day, Instant.now(clockProvider.clock), tomorrow, yesterday)
        } else {
            null
        }
        val ayahsToday = runCatching { container.database.quranDao().dailyReading(date.toString())?.ayahsRead ?: 0 }
            .getOrDefault(0)
        val position = runCatching { container.database.quranDao().readingPositionOnce() }.getOrNull()

        return HomeUiState(
            loading = false,
            settings = settings,
            day = dayState.day,
            progress = progress,
            today = date,
            hijri = HijriCalendar.fromGregorian(date, settings.hijriOffsetDays),
            fromCache = dayState.fromCache,
            fellBackToLocal = dayState.fellBackToLocal,
            errorMessage = dayState.providerErrorMessage,
            lastReadSurah = position?.surah,
            lastReadAyah = position?.ayah,
            ayahsToday = ayahsToday,
            dailyGoal = settings.dailyAyahGoal,
            zone = zone,
        )
    }

    val readingPosition: StateFlow<ReadingPositionEntity?> = container.quran.readingPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
