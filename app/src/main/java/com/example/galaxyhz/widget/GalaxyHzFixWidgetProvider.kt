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

/** 1x1 widget: one tap runs the verified anti-flicker lock pipeline. */
class GalaxyHzFixWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_fix)
        views.setOnClickPendingIntent(
            R.id.fix_root,
            PendingIntent.getBroadcast(
                context, 2,
                Intent(context, GalaxyHzFixWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(EXTRA, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (!intent.getBooleanExtra(EXTRA, false)) return
        val pending = goAsync()
        scope.launch {
            try { WidgetActions.fix(context) } finally { pending.finish() }
        }
    }

    private companion object {
        const val EXTRA = "com.example.galaxyhz.widget.FIX"
    }
}
