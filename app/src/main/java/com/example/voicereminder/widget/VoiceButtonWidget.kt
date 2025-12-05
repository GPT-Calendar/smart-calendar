package com.example.voicereminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.voicereminder.R

/**
 * Voice Button Widget - Ultra simple, one-tap voice input
 * 
 * Just a mic icon. Tap it, speak your command, done.
 * No background, no text - pure minimal design.
 */
class VoiceButtonWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_VOICE = "com.example.voicereminder.widget.VOICE_TAP"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_VOICE) {
            // Start voice input directly
            val voiceIntent = Intent(context, WidgetInputActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("start_voice", true)
            }
            context.startActivity(voiceIntent)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_voice_button)

        // Tap mic to start voice
        val voiceIntent = Intent(context, VoiceButtonWidget::class.java).apply {
            action = ACTION_VOICE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_voice_mic, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_voice_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
