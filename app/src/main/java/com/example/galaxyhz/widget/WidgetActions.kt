package com.example.galaxyhz.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.galaxyhz.R
import com.example.galaxyhz.data.AdaptiveMode
import com.example.galaxyhz.data.DeviceProfiles
import com.example.galaxyhz.data.RefreshRateManager

/** Logic shared by all GalaxyHz widgets. */
object WidgetActions {

    suspend fun applyHz(context: Context, hz: Int) {
        val profile = DeviceProfiles.detect()
        val modes = RefreshRateManager.discoverPanelModes(profile)
        val mode = modes.firstOrNull { it.hz == hz && !it.experimental }
            ?: modes.firstOrNull { it.hz == hz }
        if (mode != null) {
            RefreshRateManager.applyMode(profile, mode.width, mode.height, mode.hz, mode.panelIndex)
        } else {
            RefreshRateManager.applyMode(profile, 1080, 2400, hz, null)
        }
        refreshRates(context)
    }

    suspend fun cycle(context: Context) {
        val current = RefreshRateManager.actualRenderRate()
        val next = when {
            current >= 115 -> 96
            current >= 90 -> 60
            else -> 120
        }
        applyHz(context, next)
    }

    /** One-tap anti-flicker fix: fixed-rate props, verified pipeline from the app. */
    suspend fun fix(context: Context) {
        RefreshRateManager.applyAdaptiveMode(DeviceProfiles.detect(), AdaptiveMode.FIXED)
        RefreshRateManager.lockRefreshRate(DeviceProfiles.detect())
    }

    /** Pushes the live render rate into every widget's number view. */
    suspend fun refreshRates(context: Context) {
        val hz = RefreshRateManager.actualRenderRate().takeIf { it > 0 } ?: return
        val manager = context.getSystemService(Context.APPWIDGET_SERVICE) as? AppWidgetManager
            ?: AppWidgetManager.getInstance(context)
        val mainViews = RemoteViews(context.packageName, R.layout.widget_galaxy_hz)
        mainViews.setTextViewText(R.id.widget_current, "$hz")
        manager.updateAppWidget(
            ComponentName(context, GalaxyHzWidgetProvider::class.java), mainViews
        )
        val cycleViews = RemoteViews(context.packageName, R.layout.widget_cycle)
        cycleViews.setTextViewText(R.id.cycle_rate, "$hz")
        manager.updateAppWidget(
            ComponentName(context, GalaxyHzCycleWidgetProvider::class.java), cycleViews
        )
    }
}
