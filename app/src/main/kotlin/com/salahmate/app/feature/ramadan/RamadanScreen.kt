package com.salahmate.app.feature.ramadan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.AppContainer
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.EmptyState
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.util.HijriCalendar
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.RamadanCalendar
import com.salahmate.app.domain.prayer.RamadanDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RamadanUiState(
    val loading: Boolean = true,
    val hijriYear: Int = 1448,
    val days: List<RamadanDay> = emptyList(),
    val today: RamadanDay? = null,
    val error: String? = null,
)

/**
 * Ramadan / suhoor / iftar view.
 *
 * Imsak, Fajr, Maghrib and Iftar are shown as four separate times. Imsak is a precautionary
 * cut-off, not the start of the fast; iftar is when the fast ends at Maghrib. The app makes no
 * moon-sighting ruling — the month start follows the configured Hijri offset.
 */
class RamadanViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(RamadanUiState())
    val ui: StateFlow<RamadanUiState> = _ui

    fun load(settings: UserSettings) {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val today = LocalDate.now()
            val hijri = HijriCalendar.fromGregorian(today, settings.hijriOffsetDays)
            // Show the upcoming Ramadan when the current Hijri year's Ramadan has already ended.
            val year = if (hijri.month > 9) hijri.year + 1 else hijri.year
            val scheduleCache = mutableMapOf<LocalDate, DayPrayerTimes?>()
            val days = RamadanCalendar.buildDays(
                hijriYear = year,
                offsetDays = settings.hijriOffsetDays,
                imsakOffsetMinutes = settings.imsakOffsetMinutes,
                iftarOffsetMinutes = settings.iftarOffsetMinutes,
            ) { date ->
                scheduleCache[date] ?: run {
                    val state = container.prayerTimes.loadDay(settings, date)
                    scheduleCache[date] = state.day
                    state.day
                }
            }
            _ui.value = RamadanUiState(
                loading = false,
                hijriYear = year,
                days = days,
                today = days.firstOrNull { it.gregorianDate == today },
                error = if (days.isEmpty()) {
                    "No prayer times could be calculated for this month. Check your location and provider settings."
                } else null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamadanScreen(viewModel: RamadanViewModel, settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(settings.hijriOffsetDays, settings.latitude, settings.longitude, settings.providerMode) {
        viewModel.load(settings)
    }

    val zone = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    val dateFormat = DateTimeFormatter.ofPattern("d MMM", if (bn) Locale("bn", "BD") else Locale.ENGLISH)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "রমজান", "Ramadan") + " ${ui.hijriYear}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        when {
            ui.loading -> Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(t(bn, "সময়সূচি তৈরি হচ্ছে…", "Building the schedule…"))
            }

            ui.days.isEmpty() -> EmptyState(
                title = t(bn, "সময়সূচি তৈরি করা যায়নি", "Schedule unavailable"),
                message = ui.error ?: "",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val today = ui.today
                    if (today != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                    t(bn, "আজকের রোজা", "Today") + " · " + t(bn, "দিন ", "day ") + "${today.hijriDay}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    TimeCell(t(bn, "সেহরি শেষ (ইমসাক)", "Imsak"), today.imsak.atZone(zone).format(timeFormat), Modifier.weight(1f))
                                    TimeCell(t(bn, "ফজর", "Fajr"), today.fajr.atZone(zone).format(timeFormat), Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    TimeCell(t(bn, "মাগরিব", "Maghrib"), today.maghrib.atZone(zone).format(timeFormat), Modifier.weight(1f))
                                    TimeCell(t(bn, "ইফতার", "Iftar"), today.iftar.atZone(zone).format(timeFormat), Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    t(bn, "রোজার দৈর্ঘ্য: ", "Fast length: ") +
                                        "${today.fastingLength.toHours()}h ${today.fastingLength.toMinutes() % 60}m",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                item {
                    InfoBanner(
                        text = t(
                            bn,
                            "ইমসাক হলো সতর্কতামূলক বিরত, রোজা শুরু হয় ফজরের ওয়াক্তে; ইফতার শুরু হয় মাগরিবে। রমজান শুরুর তারিখ চাঁদ দেখার ওপর নির্ভর করে — এখানে আপনার হিজরি অফসেট অনুসরণ করা হয়েছে।",
                            "Imsak is a precautionary cut-off; the fast begins at Fajr and ends at Maghrib. The start of Ramadan depends on local moon sighting — this screen follows your configured Hijri offset, not a ruling.",
                        ),
                        icon = Icons.Outlined.Info,
                    )
                }

                item {
                    AppCard {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(t(bn, "রোজা", "Day"), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.7f))
                            Text(t(bn, "তারিখ", "Date"), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.2f))
                            Text(t(bn, "ইমসাক", "Imsak"), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text(t(bn, "ইফতার", "Iftar"), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }

                items(ui.days, key = { it.gregorianDate.toString() }) { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${day.hijriDay}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.7f))
                        Text(day.gregorianDate.format(dateFormat), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
                        Text(day.imsak.atZone(zone).format(timeFormat), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(day.iftar.atZone(zone).format(timeFormat), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Text(
                        t(
                            bn,
                            "সময়গুলো আপনার অবস্থান ও নির্বাচিত গণনা পদ্ধতির ভিত্তিতে হিসাব করা; স্থানীয় মসজিদ বা সরকারি সময়সূচির সাথে কিছু মিনিট পার্থক্য হতে পারে।",
                            "Times are calculated from your location and the selected method; they may differ by a few minutes from your local mosque or official timetable.",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Start)
    }
}
