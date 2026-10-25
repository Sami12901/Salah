package com.salahmate.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.salahmate.app.AppContainer

/** Thin indirection so receivers can refresh the widget without knowing the widget framework. */
object PrayerWidgetUpdater {
    suspend fun updateNow(context: Context, container: AppContainer) {
        runCatching { PrayerWidget().updateAll(context) }
    }
}
