package com.salahmate.app.feature.prayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.AppContainer
import com.salahmate.app.core.db.PrayerLogEntity
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.PrayerKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TrackingDay(val date: LocalDate, val logs: Map<String, Boolean>)

/**
 * Prayer tracking. Everything stays in the local Room database: no account, no upload, no analytics.
 * The log is a personal record, and the UI never implies it is an assessment of the prayer itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackingViewModel(private val container: AppContainer) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = selectedDate

    val logs: StateFlow<Map<String, Boolean>> = selectedDate.flatMapLatest { date ->
        container.database.prayerTrackingDao().forDate(date.toString())
            .map { rows -> rows.associate { it.prayer to it.completed } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Last 14 days summary. */
    val recent: StateFlow<Map<String, Int>> = run {
        val to = LocalDate.now()
        val from = to.minusDays(13)
        container.database.prayerTrackingDao().between(from.toString(), to.toString())
            .map { rows -> rows.filter { it.completed }.groupBy({ it.date }, { 1 }).mapValues { it.value.size } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun toggle(prayer: PrayerKey) {
        val date = selectedDate.value.toString()
        val current = logs.value[prayer.id] ?: false
        viewModelScope.launch {
            container.database.prayerTrackingDao().upsert(
                PrayerLogEntity(
                    date = date,
                    prayer = prayer.id,
                    completed = !current,
                    note = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun last14Days(): List<LocalDate> {
        val today = LocalDate.now()
        return (13 downTo 0).map { today.minusDays(it.toLong()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTrackingScreen(viewModel: PrayerTrackingViewModel, settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val date by viewModel.date.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val days = remember14(viewModel)
    val formatted = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", if (bn) Locale("bn", "BD") else Locale.ENGLISH))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "নামাজ ট্র্যাকিং", "Prayer tracking")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(days) { _, day ->
                        val isSelected = day == date
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { viewModel.selectDate(day) },
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    day.dayOfWeek.name.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text("${day.dayOfMonth}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${recent[day.toString()] ?: 0}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                AppCard {
                    Text(formatted, style = MaterialTheme.typography.titleMedium)
                    Text(
                        t(bn, "শুধু আপনার ডিভাইসে সংরক্ষিত — কোথাও পাঠানো হয় না।", "Stored only on this device — never uploaded."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    PrayerKey.obligatory.forEach { prayer ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = logs[prayer.id] == true,
                                onCheckedChange = { viewModel.toggle(prayer) },
                            )
                            Text(
                                if (bn) prayer.labelBn else prayer.labelEn,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "গত ১৪ দিন", "Last 14 days"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        days.forEach { day ->
                            val count = recent[day.toString()] ?: 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(26.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = when {
                                            count >= 5 -> MaterialTheme.colorScheme.primary
                                            count > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    ) {
                                        Text(
                                            if (count > 0) "$count" else "",
                                            modifier = Modifier.padding(4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (count >= 5) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                InfoBanner(
                    text = t(
                        bn,
                        "এই ট্র্যাকিং একটি ব্যক্তিগত রেকর্ড রাখার সুবিধা; এটি নামাজের বৈধতা বা কবুল হওয়ার কোনো বিচার নয়।",
                        "This tracker is a personal record-keeping aid; it is not a judgement about the validity or acceptance of any prayer.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }
        }
    }
}

@Composable
private fun remember14(viewModel: PrayerTrackingViewModel): List<LocalDate> =
    remember { viewModel.last14Days() }
