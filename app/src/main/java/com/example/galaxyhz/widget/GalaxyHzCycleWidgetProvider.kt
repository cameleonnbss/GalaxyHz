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

/** 1x1 widget: shows the live rate; tapping cycles 120 -> 96 -> 60. */
class GalaxyHzCycleWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_cycle)
        views.setOnClickPendingIntent(
            R.id.cycle_root,
            PendingIntent.getBroadcast(
                context, 1,
                Intent(context, GalaxyHzCycleWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(EXTRA, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
        scope.launch { WidgetActions.refreshRates(context) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (!intent.getBooleanExtra(EXTRA, false)) return
        val pending = goAsync()
        scope.launch {
            try { WidgetActions.cycle(context) } finally { pending.finish() }
        }
    }

    private companion object {
        const val EXTRA = "com.example.galaxyhz.widget.CYCLE"
    }
}
