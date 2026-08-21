package com.example.widget

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.repository.DrawingRepository
import com.example.util.WallpaperHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PartnerDrawingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PartnerDrawingWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as Application
                    val repository = DrawingRepository.getInstance(app)

                    val bitmap = WallpaperHelper.createDrawingBitmap(
                        context = context,
                        strokes = repository.strokes.value,
                        stickers = repository.placedStickers.value,
                        config = repository.lockscreenConfig.value,
                        width = 720,
                        height = 720,
                        includeBackground = true
                    )

                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    for (widgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.partner_widget_layout)
                        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    // Ignore widget update errors gracefully
                }
            }
        }
    }
}
