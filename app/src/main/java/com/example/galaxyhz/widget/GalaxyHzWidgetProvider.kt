package com.example.galaxyhz.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.galaxyhz.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 4x1 widget: tap 60 / 96 / 120 to apply that rate directly, or the big
 * number to cycle 120 -> 96 -> 60. Works without opening the app.
 */
class GalaxyHzWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, buildViews(context))
        scope.launch { WidgetActions.refreshRates(context) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val hz = intent.getIntExtra(EXTRA_HZ, Int.MIN_VALUE)
        if (hz == Int.MIN_VALUE) return
        // Keep the process alive until the root work completes.
        val pending = goAsync()
        scope.launch {
            try {
                if (hz == CYCLE) WidgetActions.cycle(context)
                else WidgetActions.applyHz(context, hz)
            } finally {
                pending.finish()
            }
        }
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
