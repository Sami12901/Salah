package com.salahmate.app.feature.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.EmptyState
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.util.HijriCalendar
import com.salahmate.app.domain.prayer.TimetableRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Monthly timetable. Every value here is calculated with the user's own method and location — the
 * header states both so a printed copy cannot be mistaken for a mosque's official timetable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    settings: UserSettings,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val bn = settings.isBanglaUi
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    LaunchedEffect(settings.latitude, settings.longitude, settings.method, settings.madhab, settings.providerMode) {
        viewModel.load(settings, force = true)
    }

    val monthLabel = remember(ui.year, ui.month, bn) {
        val locale = if (bn) Locale("bn", "BD") else Locale.ENGLISH
        LocalDate.of(ui.year, ui.month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }
    val methodLabel = if (bn) settings.method.labelBn else settings.method.labelEn
    val madhabLabel = if (bn) settings.madhab.labelBn else settings.madhab.labelEn

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(t(bn, "মাসিক সময়সূচি", "Monthly timetable"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            settings.locationLabel,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.shiftMonth(settings, -1) }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = t(bn, "আগের মাস", "Previous month"))
                        }
                        Text(
                            monthLabel,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.shiftMonth(settings, 1) }) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = t(bn, "পরের মাস", "Next month"))
                        }
                    }
                    Text(
                        t(bn, "গণনা: ", "Calculation: ") + methodLabel + " · " + t(bn, "আসর: ", "Asr: ") + madhabLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        t(bn, "সময় অঞ্চল: ", "Timezone: ") + settings.timeZoneId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.goToCurrentMonth(settings) }) {
                            Text(t(bn, "এই মাস", "This month"))
                        }
                        TextButton(onClick = onOpenSettings) {
                            Text(t(bn, "গণনা পদ্ধতি বদলান", "Change calculation"))
                        }
                    }
                }
            }

            item {
                TimetableHeaderRow(bn)
            }

            when {
                ui.loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }

                ui.error != null -> item {
                    EmptyState(
                        title = t(bn, "সময়সূচি নেই", "Timetable unavailable"),
                        message = ui.error ?: "",
                        icon = Icons.Outlined.Info,
                    )
                }

                else -> items(ui.rows, key = { it.date.toString() }) { row ->
                    TimetableRowView(row = row, today = today, bn = bn)
                }
            }

            if (ui.missing.isNotEmpty() && !ui.loading) {
                item {
                    InfoBanner(
                        text = t(
                            bn,
                            "${ui.missing.size} দিনের সময় পাওয়া যায়নি (নিচে ফাঁকা দেখানো হয়েছে)। সম্ভবত উচ্চ অক্ষাংশে সূর্যাস্ত/ফজর গণনা করা যায়নি — হাই-ল্যাটিটিউড নিয়ম বদলে দেখুন।",
                            "Times are missing for ${ui.missing.size} day(s) and are shown as gaps. High-latitude locations can fail to produce a Fajr/Isha angle — try a different high-latitude rule.",
                        ),
                        icon = Icons.Outlined.Info,
                    )
                }
            }

            item {
                InfoBanner(
                    text = t(
                        bn,
                        "এটি আপনার নির্বাচিত পদ্ধতিতে হিসাব করা সময়সূচি। স্থানীয় মসজিদ বা সরকারি সময়সূচির সাথে কয়েক মিনিট পার্থক্য হতে পারে — প্রয়োজনে Settings থেকে প্রতি নামাজের অ্যাডজাস্টমেন্ট দিন।",
                        "These times are calculated with the method you selected. They may differ by a few minutes from a local mosque or official timetable — per-prayer adjustments are available in Settings.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }
        }
    }
}

@Composable
private fun TimetableHeaderRow(bn: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        HeaderCell(t(bn, "তারিখ", "Date"), 1.1f)
        HeaderCell(t(bn, "ফজর", "Fajr"), 1f)
        HeaderCell(t(bn, "সূর্যোদয়", "Sunrise"), 1f)
        HeaderCell(t(bn, "যোহর", "Dhuhr"), 1f)
        HeaderCell(t(bn, "আসর", "Asr"), 1f)
        HeaderCell(t(bn, "মাগরিব", "Maghrib"), 1f)
        HeaderCell(t(bn, "এশা", "Isha"), 1f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun TimetableRowView(row: TimetableRow, today: LocalDate, bn: Boolean) {
    val isToday = row.date == today
    val dateLabel = remember(row.date, bn) {
        row.date.format(DateTimeFormatter.ofPattern("d MMM", if (bn) Locale("bn", "BD") else Locale.ENGLISH))
    }
    val background = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .semantics {
                contentDescription = buildString {
                    append(dateLabel)
                    append(", hijri day ${row.hijriDay}")
                    if (isToday) append(", today")
                    append(if (row.complete) ", all prayers available" else ", incomplete times")
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            Text(dateLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
            Text(
                "${row.hijriDay} " + HijriCalendar.monthName(row.hijriMonth, bn).take(4),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TimeCell(row.fajr)
        TimeCell(row.sunrise)
        TimeCell(row.dhuhr)
        TimeCell(row.asr)
        TimeCell(row.maghrib)
        TimeCell(row.isha)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TimeCell(value: String?) {
    Text(
        value ?: "—",
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        modifier = Modifier.weight(1f),
        color = if (value == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}
