package com.example.galaxyhz.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.galaxyhz.R
import com.example.galaxyhz.data.DeviceProfiles
import com.example.galaxyhz.data.RefreshRateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen widget: tap 60 / 96 / 120 to apply that rate directly, or the
 * big number to cycle 120 -> 96 -> 60. Works without opening the app.
 */
class GalaxyHzWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, buildViews(context))
        scope.launch { refreshCurrent(context) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val hz = intent.getIntExtra(EXTRA_HZ, Int.MIN_VALUE)
        if (hz == Int.MIN_VALUE) return
        scope.launch {
            val profile = DeviceProfiles.detect()
            if (hz == CYCLE) {
                val current = RefreshRateManager.actualRenderRate()
                val next = when {
                    current >= 115 -> 96
                    current >= 90 -> 60
                    else -> 120
                }
                apply(context, profile, next)
            } else {
                apply(context, profile, hz)
            }
            refreshCurrent(context)
        }
    }

    private suspend fun apply(
        context: Context,
        profile: com.example.galaxyhz.data.DeviceProfile,
        hz: Int
    ) {
        val modes = RefreshRateManager.discoverPanelModes(profile)
        val mode = modes.firstOrNull { it.hz == hz && !it.experimental }
            ?: modes.firstOrNull { it.hz == hz }
        if (mode != null) {
            RefreshRateManager.applyMode(
                profile, mode.width, mode.height, mode.hz, mode.panelIndex
            )
        } else {
            RefreshRateManager.applyMode(profile, 1080, 2400, hz, null)
        }
    }

    /** Reads the live render rate back into the widget's big number. */
    private suspend fun refreshCurrent(context: Context) {
        val hz = RefreshRateManager.actualRenderRate().takeIf { it > 0 } ?: return
        val views = buildViews(context)
        views.setTextViewText(R.id.widget_current, "$hz")
        AppWidgetManager.getInstance(context).updateAppWidget(
            ComponentName(context, GalaxyHzWidgetProvider::class.java), views
        )
    }

    private fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_galaxy_hz)
        views.setOnClickPendingIntent(R.id.widget_current, pi(context, CYCLE))
        views.setOnClickPendingIntent(R.id.widget_b120, pi(context, 120))
        views.setOnClickPendingIntent(R.id.widget_b96, pi(context, 96))
        views.setOnClickPendingIntent(R.id.widget_b60, pi(context, 60))
        return views
    }

    private fun pi(context: Context, hz: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        hz,
        Intent(context, GalaxyHzWidgetProvider::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(EXTRA_HZ, hz),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        const val EXTRA_HZ = "com.example.galaxyhz.widget.HZ"
        const val CYCLE = 0
    }
}
