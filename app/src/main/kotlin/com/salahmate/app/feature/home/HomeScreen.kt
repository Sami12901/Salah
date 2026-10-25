package com.salahmate.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import com.salahmate.app.Routes
import com.salahmate.app.core.design.Emerald700
import com.salahmate.app.core.design.Emerald900
import com.salahmate.app.core.design.Gold300
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.components.StatusPill
import com.salahmate.app.core.design.prayerAccent
import com.salahmate.app.core.design.t
import com.salahmate.app.core.util.formatCountdown
import com.salahmate.app.core.util.formatDateFull
import com.salahmate.app.core.util.formatTime
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerProgress
import com.salahmate.app.domain.prayer.PrayerSchedule
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Home dashboard: real prayer data, a ticking countdown that only recomposes its own text, reading
 * progress and quick access to every module.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowProvider: () -> Instant = { Instant.now() },
) {
    val bn = state.settings.isBanglaUi
    val locale = if (bn) Locale("bn", "BD") else Locale.ENGLISH

    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HomeHeader(
                location = if (bn) state.settings.locationLabel else state.settings.locationLabel,
                dateLine = formatDateFull(state.today, locale),
                hijriLine = state.hijri?.let { if (bn) it.formattedBn else it.formattedEn }.orEmpty(),
                bangla = bn,
                onLocationClick = { onNavigate(Routes.PRAYER_SETTINGS) },
                onSettingsClick = { onNavigate(Routes.SETTINGS) },
            )
        }

        item {
            val day = state.day
            if (day == null || !day.hasAllObligatory) {
                AppCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        t(bn, "নামাজের সময় পাওয়া যায়নি", "Prayer times unavailable"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.errorMessage ?: t(bn, "গণনা ব্যর্থ হয়েছে। সেটিংসে স্থান ও পদ্ধতি যাচাই করুন।", "Calculation failed. Check your location and method in settings."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            } else {
                NextPrayerHero(
                    progress = state.progress,
                    day = day,
                    bangla = bn,
                    locale = locale,
                    nowProvider = nowProvider,
                )
            }
        }

        item { PrayerTimesCard(day = state.day, progress = state.progress, bangla = bn, locale = locale) }

        if (state.day != null && state.day.hasAllObligatory) {
            item { SunAndNightCard(day = state.day, bangla = bn, locale = locale) }
        }

        // Provenance and honesty labels: never present a cached or fallback schedule as live data.
        if (state.fromCache) {
            item {
                InfoBanner(
                    text = t(bn, "সংরক্ষিত সময় দেখানো হচ্ছে — ইন্টারনেট সংযোগ নেই।", "Showing cached times — no network connection."),
                    icon = Icons.Outlined.CloudOff,
                )
            }
        } else if (state.fellBackToLocal) {
            item {
                InfoBanner(
                    text = t(bn, "এপিআই সংযোগ ব্যর্থ; ডিভাইসে গণনা করা সময় দেখানো হচ্ছে।", "API unavailable; showing on-device calculation."),
                    icon = Icons.Outlined.CloudOff,
                )
            }
        }

        item {
            ReadingProgressCard(
                state = state,
                bangla = bn,
                locale = locale,
                onContinue = {
                    val surah = state.lastReadSurah ?: 1
                    val ayah = state.lastReadAyah
                    onNavigate(Routes.reader(surah, ayah))
                },
                onQuran = { onNavigate(Routes.QURAN) },
            )
        }

        item {
            Text(
                t(bn, "দ্রুত প্রবেশ", "Quick access"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        item {
            QuickAccessGrid(bangla = bn, onNavigate = onNavigate)
        }

        item {
            AppCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    t(bn, "গণনা পদ্ধতি: ", "Calculation method: ") +
                        (if (bn) state.settings.method.labelBn else state.settings.method.labelEn) +
                        " · " + (if (bn) state.settings.madhab.labelBn else state.settings.madhab.labelEn),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    t(
                        bn,
                        "গণনাকৃত সময় স্থানীয় মসজিদ বা সরকারি সময়সূচির সাথে কিছুটা ভিন্ন হতে পারে। আপনার বিশ্বস্ত স্থানীয় সময়সূচির সাথে মিলিয়ে নিন; প্রয়োজনে মিনিট সমন্বয় ব্যবহার করুন।",
                        "Calculated times can differ slightly from local mosque or government timetables. Please check them against a trusted local timetable and use the minute adjustments if needed.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    location: String,
    dateLine: String,
    hijriLine: String,
    bangla: Boolean,
    onLocationClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onLocationClick)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .semantics { contentDescription = "Location: $location. Tap to change." },
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(location, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(dateLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (hijriLine.isNotBlank()) {
                Text(
                    hijriLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onSettingsClick)
                .semantics { contentDescription = if (bangla) "সেটিংস" else "Settings" },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(21.dp))
            }
        }
    }
}

/**
 * Hero card with the next prayer and a live countdown.
 *
 * The countdown is isolated in [CountdownText] so a one-second tick recomposes a single Text node
 * instead of the dashboard.
 */
@Composable
private fun NextPrayerHero(
    progress: PrayerProgress?,
    day: DayPrayerTimes,
    bangla: Boolean,
    locale: Locale,
    nowProvider: () -> Instant,
) {
    val zone: ZoneId = day.zoneId
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Emerald900, Emerald700),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        t(bangla, "পরবর্তী নামাজ", "Next prayer"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Gold300,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (progress == null) "—" else if (bangla) progress.next.labelBn else progress.next.labelEn,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    if (progress != null) {
                        Text(
                            formatTime(progress.nextTime, zone, locale) +
                                if (progress.isNextTomorrow) t(bangla, " (আগামীকাল)", " (tomorrow)") else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                        Spacer(Modifier.height(8.dp))
                        CountdownText(
                            target = progress.nextTime,
                            bangla = bangla,
                            nowProvider = nowProvider,
                            color = Gold300,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = (if (progress.current == null) "" else
                                t(bangla, "চলমান: ", "Current: ") + (if (bangla) progress.current.labelBn else progress.current.labelEn)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                PrayerWindowRing(progress = progress, nowProvider = nowProvider)
            }
        }
    }
}

@Composable
private fun PrayerWindowRing(progress: PrayerProgress?, nowProvider: () -> Instant) {
    val fraction by produceState(initialValue = progress?.let { PrayerSchedule.windowProgress(it) } ?: 0f, progress) {
        while (true) {
            value = progress?.let { PrayerSchedule.windowProgress(it) } ?: 0f
            delay(1000)
        }
    }
    val ringDescription = t(
        true,
        "চলমান নামাজের সময়ের অগ্রগতি ${(fraction * 100).toInt()} শতাংশ",
        "Progress through the current prayer window",
    )
    Box(modifier = Modifier.size(92.dp).semantics { contentDescription = ringDescription }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Gold300,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Icon(
            Icons.Outlined.NightsStay,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(30.dp),
        )
    }
}

/**
 * Live countdown. Recomputes every second but exposes the result through a scoped state object, so
 * only this Text recomposes — not the dashboard around it.
 */
@Composable
fun CountdownText(
    target: Instant,
    bangla: Boolean,
    nowProvider: () -> Instant,
    color: Color = MaterialTheme.colorScheme.primary,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val remaining by produceState(initialValue = Duration.between(nowProvider(), target), target) {
        while (true) {
            value = Duration.between(nowProvider(), target)
            delay(1000)
        }
    }
    val text = if (remaining.isNegative) {
        t(bangla, "সময় হয়ে গেছে", "Time passed")
    } else {
        formatCountdown(remaining) + t(bangla, " বাকি", " remaining")
    }
    Text(
        text = text,
        style = style,
        color = color,
        modifier = Modifier.semantics {
            contentDescription = if (bangla) "বাকি সময় $text" else "Time remaining: $text"
        },
    )
}

@Composable
private fun PrayerTimesCard(day: DayPrayerTimes?, progress: PrayerProgress?, bangla: Boolean, locale: Locale) {
    AppCard {
        Text(t(bangla, "আজকের সময়সূচি", "Today's schedule"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (day == null) {
            Text(t(bangla, "কোনো তথ্য নেই", "No data"), style = MaterialTheme.typography.bodyMedium)
            return@AppCard
        }
        val rows = listOf(
            PrayerKey.FAJR, PrayerKey.SUNRISE, PrayerKey.DHUHR,
            PrayerKey.ASR, PrayerKey.MAGHRIB, PrayerKey.ISHA,
        )
        rows.forEach { key ->
            val instant = day.time(key)
            val isCurrent = progress?.current == key
            val isNext = progress?.next == key
            PrayerTimeRow(
                key = key,
                time = instant?.let { formatTime(it, day.zoneId, locale) } ?: "—",
                isCurrent = isCurrent,
                isNext = isNext,
                isTomorrow = isNext && progress?.isNextTomorrow == true,
                bangla = bangla,
            )
        }
    }
}

@Composable
private fun PrayerTimeRow(
    key: PrayerKey,
    time: String,
    isCurrent: Boolean,
    isNext: Boolean,
    isTomorrow: Boolean,
    bangla: Boolean,
) {
    val accent = prayerAccent(key.id)
    val label = if (bangla) key.labelBn else key.labelEn
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics {
                contentDescription = "$label $time" +
                    if (isCurrent) (if (bangla) ", চলমান" else ", current") else "" +
                        if (isNext) (if (bangla) ", পরবর্তী" else ", next") else ""
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (key == PrayerKey.SUNRISE) accent.copy(alpha = 0.5f) else accent),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent || isNext) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (isCurrent) {
            StatusPill(text = t(bangla, "চলমান", "Now"), color = accent)
            Spacer(Modifier.width(8.dp))
        } else if (isNext) {
            StatusPill(text = t(bangla, "পরবর্তী", "Next") + if (isTomorrow) " ·" else "", color = accent)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            time,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent || isNext) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCurrent || isNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SunAndNightCard(day: DayPrayerTimes, bangla: Boolean, locale: Locale) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SunFact(
                icon = Icons.Outlined.WbSunny,
                label = t(bangla, "সূর্যোদয়", "Sunrise"),
                value = day.time(PrayerKey.SUNRISE)?.let { formatTime(it, day.zoneId, locale) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            SunFact(
                icon = Icons.Outlined.WbSunny,
                label = t(bangla, "সূর্যাস্ত", "Sunset"),
                value = day.sunset?.let { formatTime(it, day.zoneId, locale) } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SunFact(
                icon = Icons.Outlined.NightsStay,
                label = t(bangla, "মধ্যরাত", "Midnight"),
                value = day.midnight?.let { formatTime(it, day.zoneId, locale) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            SunFact(
                icon = Icons.Outlined.NightsStay,
                label = t(bangla, "রাতের শেষ তৃতীয়াংশ", "Last third"),
                value = day.lastThirdOfNight?.let { formatTime(it, day.zoneId, locale) } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SunFact(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReadingProgressCard(
    state: HomeUiState,
    bangla: Boolean,
    locale: Locale,
    onContinue: () -> Unit,
    onQuran: () -> Unit,
) {
    val goal = state.dailyGoal.coerceAtLeast(1)
    val fraction = (state.ayahsToday.toFloat() / goal).coerceIn(0f, 1f)
    AppCard {
        Text(t(bangla, "কুরআন পাঠ", "Quran reading"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            t(bangla, "আজ পড়া হয়েছে ${state.ayahsToday} আয়াত · লক্ষ্য $goal", "Read today: ${state.ayahsToday} ayahs · goal $goal"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (state.lastReadSurah != null) {
                    t(bangla, "শেষ অবস্থান: সূরা ${state.lastReadSurah}, আয়াত ${state.lastReadAyah}", "Last read: Surah ${state.lastReadSurah}, ayah ${state.lastReadAyah}")
                } else {
                    t(bangla, "এখনো পড়া শুরু হয়নি", "No reading position yet")
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryAction(
                text = if (state.lastReadSurah != null) t(bangla, "পড়া চালিয়ে যান", "Continue reading") else t(bangla, "পড়া শুরু করুন", "Start reading"),
                onClick = if (state.lastReadSurah != null) onContinue else onQuran,
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                text = t(bangla, "সব সূরা", "All surahs"),
                onClick = onQuran,
                filled = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    if (filled) {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }
    } else {
        androidx.compose.material3.OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }
    }
}

private data class QuickAction(val route: String, val icon: ImageVector, val labelEn: String, val labelBn: String)

private val quickActions = listOf(
    QuickAction(Routes.QURAN, Icons.Outlined.MenuBook, "Quran", "কুরআন"),
    QuickAction(Routes.QIBLA, Icons.Outlined.Explore, "Qibla", "কিবলা"),
    QuickAction(Routes.DUA, Icons.Outlined.SelfImprovement, "Dua", "দুআ"),
    QuickAction(Routes.TASBIH, Icons.Outlined.TouchApp, "Tasbih", "তাসবিহ"),
    QuickAction(Routes.HADITH, Icons.Outlined.AutoStories, "Hadith", "হাদিস"),
    QuickAction(Routes.ZAKAT, Icons.Outlined.Calculate, "Zakat", "যাকাত"),
    QuickAction(Routes.RAMADAN, Icons.Outlined.NightsStay, "Ramadan", "রমজান"),
    QuickAction(Routes.HIFZ, Icons.Outlined.School, "Hifz", "হিফজ"),
    QuickAction(Routes.TIMETABLE, Icons.Outlined.Schedule, "Timetable", "সময়সূচি"),
    QuickAction(Routes.CALENDAR, Icons.Outlined.CalendarMonth, "Calendar", "ক্যালেন্ডার"),
    QuickAction(Routes.PRAYER_TRACKING, Icons.Outlined.History, "Tracking", "ট্র্যাকিং"),
    QuickAction(Routes.MOSQUE, Icons.Outlined.Mosque, "Mosque", "মসজিদ"),
    QuickAction(Routes.LEARNING, Icons.Outlined.AutoStories, "Learning", "শিক্ষা"),
)

/**
 * Quick-access tiles. Rendered as a fixed-height grid inside a LazyColumn (no nested scrolling),
 * so the whole home screen scrolls as one list.
 */
@Composable
private fun QuickAccessGrid(bangla: Boolean, onNavigate: (String) -> Unit) {
    val columns = 4
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        quickActions.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { action ->
                    QuickTile(
                        action = action,
                        bangla = bangla,
                        onClick = { onNavigate(action.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun QuickTile(action: QuickAction, bangla: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = if (bangla) action.labelBn else action.labelEn
    Surface(
        modifier = modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(action.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 11.sp,
            )
        }
    }
}
