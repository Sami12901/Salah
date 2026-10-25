package com.salahmate.app.widget

import kotlinx.coroutines.flow.first
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.salahmate.app.SalahMateApplication
import com.salahmate.app.core.util.HijriCalendar
import com.salahmate.app.core.util.formatCountdown
import com.salahmate.app.core.util.formatTime
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Everything the widget renders, resolved once per refresh. */
data class WidgetState(
    val locationLabel: String = "",
    val hijri: String = "",
    val nextLabel: String = "",
    val nextTime: String = "",
    val countdown: String = "",
    val schedule: List<Pair<String, String>> = emptyList(),
    val method: String = "",
    val hasData: Boolean = false,
)

/**
 * Home-screen widget: next prayer, its time, the remaining countdown, today's schedule and the
 * Hijri date. Compact, medium and expanded layouts are selected by the platform via
 * [SizeMode.Responsive].
 *
 * Refresh strategy (no polling loop): refreshed when a prayer alarm fires, by the daily
 * maintenance worker, on boot/timezone change, and whenever the app updates the schedule. Between
 * refreshes the countdown text is a snapshot — a deliberate trade-off documented in the README,
 * because a ticking widget would require a persistent background process.
 */
class PrayerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, MEDIUM, EXPANDED))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent {
            GlanceTheme {
                WidgetBody(state)
            }
        }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val app = context.applicationContext as? SalahMateApplication ?: return WidgetState()
        val container = app.container
        val settings = runCatching {
            container.settings.settings.first()
        }.getOrNull() ?: return WidgetState()

        val zone = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val today = LocalDate.now(zone)
        val day = runCatching { container.prayerTimes.loadDay(settings, today).day }.getOrNull()
        val tomorrow = runCatching {
            container.prayerTimes.loadDay(settings, today.plusDays(1), allowNetwork = false).day
        }.getOrNull()
        val yesterday = runCatching {
            container.prayerTimes.loadDay(settings, today.minusDays(1), allowNetwork = false).day
        }.getOrNull()

        // All display logic lives in WidgetContent so it can be unit-tested.
        return WidgetContent.build(
            settings = settings,
            today = today,
            day = day,
            yesterday = yesterday,
            tomorrow = tomorrow,
            now = Instant.now(),
            zone = zone,
        )
    }

    companion object {
        val COMPACT = DpSize(170.dp, 100.dp)
        val MEDIUM = DpSize(250.dp, 140.dp)
        val EXPANDED = DpSize(320.dp, 230.dp)
    }
}

private val WidgetEmerald = Color(0xFF0E5C4A)
private val WidgetGold = Color(0xFFE7C76A)
private val WidgetCream = Color(0xFFFDFBF5)
private val WidgetInk = Color(0xFF14201C)
private val WidgetInkMuted = Color(0xFF42544D)

@Composable
private fun WidgetBody(state: WidgetState) {
    val size = LocalSize.current
    val expanded = size.height >= 200.dp
    val medium = size.height >= 130.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetCream))
            .padding(12.dp),
    ) {
        if (!state.hasData) {
            Text(
                text = if (state.locationLabel.isBlank()) "Open Salah to load prayer times" else state.locationLabel,
                style = TextStyle(color = ColorProvider(WidgetInk), fontSize = 13.sp),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = "Prayer times unavailable",
                style = TextStyle(color = ColorProvider(WidgetInkMuted), fontSize = 11.sp),
            )
            return@Column
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = state.nextLabel,
                    style = TextStyle(
                        color = ColorProvider(WidgetEmerald),
                        fontSize = if (expanded) 20.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "${state.nextTime}  ·  ${state.countdown}",
                    style = TextStyle(color = ColorProvider(WidgetInk), fontSize = if (expanded) 15.sp else 12.sp),
                )
            }
            Text(
                text = state.hijri,
                style = TextStyle(color = ColorProvider(WidgetGold), fontSize = 10.sp),
            )
        }

        if (medium) {
            Spacer(GlanceModifier.height(8.dp))
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                state.schedule.forEach { (label, time) ->
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = label,
                            style = TextStyle(color = ColorProvider(WidgetInkMuted), fontSize = 12.sp),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Text(
                            text = time,
                            style = TextStyle(color = ColorProvider(WidgetInk), fontSize = 12.sp),
                        )
                    }
                }
            }
        }

        if (expanded) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = state.locationLabel,
                style = TextStyle(color = ColorProvider(WidgetInkMuted), fontSize = 10.sp),
            )
            Text(
                text = state.method,
                style = TextStyle(color = ColorProvider(WidgetInkMuted), fontSize = 9.sp),
            )
        }
    }
}
