package com.salahmate.app.feature.calendar

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.components.StatusPill
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.util.HijriCalendar
import com.salahmate.app.data.calendar.EventCertainty
import com.salahmate.app.data.calendar.IslamicEvents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Islamic (Hijri) calendar with month browsing and dated events. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val zone = remember { runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault()) }
    val today = remember { LocalDate.now() }
    val todayHijri = remember(settings.hijriOffsetDays) { HijriCalendar.fromGregorian(today, settings.hijriOffsetDays) }

    var hijriYear by remember { mutableIntStateOf(todayHijri.year) }
    var hijriMonth by remember { mutableIntStateOf(todayHijri.month) }

    val monthDays = remember(hijriYear, hijriMonth, settings.hijriOffsetDays) {
        val length = HijriCalendar.monthLength(hijriYear, hijriMonth)
        (1..length).map { day ->
            day to HijriCalendar.gregorianFor(hijriYear, hijriMonth, day)
                .plusDays(settings.hijriOffsetDays.toLong())
        }
    }
    val events = remember(hijriMonth) { IslamicEvents.eventsInMonth(hijriMonth) }
    val upcoming = remember(settings.hijriOffsetDays) {
        IslamicEvents.upcoming(today, 21, settings.hijriOffsetDays) { date, offset ->
            HijriCalendar.fromGregorian(date, offset)
        }
    }
    val dateFormat = remember(bn) { DateTimeFormatter.ofPattern("d MMM yyyy", if (bn) Locale("bn", "BD") else Locale.ENGLISH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "ইসলামিক ক্যালেন্ডার", "Islamic calendar")) },
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
                    Text(
                        HijriCalendar.monthName(hijriMonth, bn) + " " + hijriYear,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        t(bn, "আজ: ", "Today: ") + todayHijri.day + " " + HijriCalendar.monthName(todayHijri.month, bn) +
                            " " + todayHijri.year + " (" + today.format(dateFormat) + ")",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (hijriMonth == 1) { hijriMonth = 12; hijriYear -= 1 } else hijriMonth -= 1
                            },
                            label = { Text(t(bn, "◀ আগের মাস", "◀ Previous")) },
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (hijriMonth == 12) { hijriMonth = 1; hijriYear += 1 } else hijriMonth += 1
                            },
                            label = { Text(t(bn, "পরের মাস ▶", "Next ▶")) },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    FilterChip(
                        selected = false,
                        onClick = { hijriYear = todayHijri.year; hijriMonth = todayHijri.month },
                        label = { Text(t(bn, "আজকের মাসে ফিরুন", "Jump to this month")) },
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item {
                    AppCard {
                        Text(t(bn, "আসন্ন দিবস", "Upcoming"), style = MaterialTheme.typography.titleMedium)
                        upcoming.forEach { (event, date) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (bn) event.titleBn else event.titleEn, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        date.format(dateFormat),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                StatusPill(
                                    text = when (event.certainty) {
                                        EventCertainty.SCRIPTURAL -> t(bn, "ধর্মীয় তারিখ", "Scriptural date")
                                        EventCertainty.OBSERVED -> t(bn, "প্রচলিত", "Commonly observed")
                                    },
                                    color = when (event.certainty) {
                                        EventCertainty.SCRIPTURAL -> MaterialTheme.colorScheme.primary
                                        EventCertainty.OBSERVED -> MaterialTheme.colorScheme.outline
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(t(bn, "এই মাসের দিনগুলো", "Days this month"), style = MaterialTheme.typography.titleMedium)
            }
            items(monthDays, key = { it.first }) { (hijriDay, gregorian) ->
                val dayEvents = events.filter { it.hijriDay == hijriDay }
                val isToday = hijriDay == todayHijri.day && hijriMonth == todayHijri.month && hijriYear == todayHijri.year
                AppCard(containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("$hijriDay", style = MaterialTheme.typography.titleMedium)
                            Text(HijriCalendar.monthName(hijriMonth, bn).take(4), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(gregorian.format(dateFormat), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.4f))
                        Column(modifier = Modifier.weight(2f)) {
                            dayEvents.forEach { event ->
                                Text(
                                    if (bn) event.titleBn else event.titleEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            item {
                InfoBanner(
                    text = IslamicEvents.moonSightingNote,
                    icon = Icons.Outlined.CalendarMonth,
                )
            }
            item {
                InfoBanner(
                    text = t(
                        bn,
                        "তারিখ হিসাব করা হয়েছে সারণিভিত্তিক (tabular) হিসাব পদ্ধতিতে, আপনার হিজরি অফসেট " +
                            "${settings.hijriOffsetDays} দিন সহ। স্থানীয় চাঁদ দেখার ভিত্তিতে এক দিন কম-বেশি হতে পারে।",
                        "Dates use the tabular Hijri calculation with your offset of ${settings.hijriOffsetDays} day(s). Local moon sighting may differ by a day.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }
        }
    }
}
