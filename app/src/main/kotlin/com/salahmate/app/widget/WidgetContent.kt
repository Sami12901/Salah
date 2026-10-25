package com.salahmate.app.widget

import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.util.HijriCalendar
import com.salahmate.app.core.util.formatCountdown
import com.salahmate.app.core.util.formatTime
import com.salahmate.app.domain.prayer.DayPrayerTimes
import com.salahmate.app.domain.prayer.PrayerKey
import com.salahmate.app.domain.prayer.PrayerSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Builds the widget's display state from an already-loaded schedule.
 *
 * Deliberately pure (no Context, no repository) so the content — which prayer is next, the
 * countdown text, the five schedule rows, the Hijri line and the empty state — can be unit-tested
 * without a device. [PrayerWidget] only loads the data and renders this.
 */
object WidgetContent {

    fun empty(settings: UserSettings): WidgetState = WidgetState(locationLabel = settings.locationLabel)

    fun build(
        settings: UserSettings,
        today: LocalDate,
        day: DayPrayerTimes?,
        yesterday: DayPrayerTimes?,
        tomorrow: DayPrayerTimes?,
        now: Instant,
        zone: ZoneId,
    ): WidgetState {
        if (day == null || !day.hasAllObligatory) return empty(settings)

        val bangla = settings.isBanglaUi
        val locale = if (bangla) Locale("bn", "BD") else Locale.ENGLISH
        val progress = PrayerSchedule.progress(day, now, tomorrow, yesterday)
        val hijri = HijriCalendar.fromGregorian(today, settings.hijriOffsetDays)

        return WidgetState(
            locationLabel = settings.locationLabel,
            hijri = if (bangla) hijri.formattedBn else hijri.formattedEn,
            nextLabel = progress?.let { if (bangla) it.next.labelBn else it.next.labelEn } ?: "",
            nextTime = progress?.let { formatTime(it.nextTime, zone, locale) } ?: "",
            countdown = progress?.remaining?.let { formatCountdown(it, includeSecondsWhenShort = false) } ?: "",
            schedule = PrayerKey.obligatory.map { key ->
                val label = if (bangla) key.labelBn else key.labelEn
                label to (day.time(key)?.let { formatTime(it, zone, locale) } ?: "—")
            },
            method = if (bangla) settings.method.labelBn else settings.method.labelEn,
            hasData = true,
        )
    }
}
