package com.example.voicereminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.voicereminder.R

/**
 * Chat Widget Provider - Interactive AI assistant widget
 * Users can type/speak directly from home screen without opening the app
 */
class ChatWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_OPEN_INPUT = "com.example.voicereminder.widget.OPEN_INPUT"
        const val ACTION_VOICE_INPUT = "com.example.voicereminder.widget.VOICE_INPUT"
        const val ACTION_QUICK_REMINDER = "com.example.voicereminder.widget.QUICK_REMINDER"
        const val ACTION_QUICK_FINANCE = "com.example.voicereminder.widget.QUICK_FINANCE"
        const val ACTION_CLEAR_CHAT = "com.example.voicereminder.widget.CLEAR_CHAT"
        const val ACTION_START_FLOATING = "com.example.voicereminder.widget.START_FLOATING"

        /**
         * Update widget with new AI response
         */
        fun updateWidgetResponse(context: Context, response: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, ChatWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

            for (widgetId in widgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_chat)
                val displayText = if (response.length > 200) {
                    response.take(197) + "..."
                } else {
                    response
                }
                views.setTextViewText(R.id.widget_response_text, displayText)
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
            }
        }

        /**
         * Reset widget to default state
         */
        fun resetWidget(context: Context) {
            updateWidgetResponse(context, "Hi! I can help with reminders and more. Tap below to chat 👇")
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_OPEN_INPUT -> {
                // Open the input overlay activity
                val inputIntent = Intent(context, WidgetInputActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(inputIntent)
            }
            ACTION_VOICE_INPUT -> {
                // Open input activity with voice mode
                val voiceIntent = Intent(context, WidgetInputActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("start_voice", true)
                }
                context.startActivity(voiceIntent)
            }
            ACTION_QUICK_REMINDER -> {
                // Send quick reminder command
                ChatWidgetService.sendMessage(context, "Remind me in 1 hour to check this")
                updateWidgetResponse(context, "⏳ Setting reminder...")
            }
            ACTION_QUICK_FINANCE -> {
                // Open finance tab in main app
                val financeIntent = Intent(context, com.example.voicereminder.presentation.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("open_tab", "finance")
                }
                context.startActivity(financeIntent)
            }
            ACTION_START_FLOATING -> {
                // Start floating chat service
                FloatingChatService.start(context)
            }
            ACTION_CLEAR_CHAT -> {
                resetWidget(context)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_chat)

            // Input area - opens input dialog
            val openInputIntent = Intent(context, ChatWidgetProvider::class.java).apply {
                action = ACTION_OPEN_INPUT
            }
            val openInputPendingIntent = PendingIntent.getBroadcast(
                context, 0, openInputIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_input_area, openInputPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_input_hint, openInputPendingIntent)

            // Voice input button
            val voiceIntent = Intent(context, ChatWidgetProvider::class.java).apply {
                action = ACTION_VOICE_INPUT
            }
            val voicePendingIntent = PendingIntent.getBroadcast(
                context, 1, voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_voice_button, voicePendingIntent)

            // Quick reminder button
            val reminderIntent = Intent(context, ChatWidgetProvider::class.java).apply {
                action = ACTION_QUICK_REMINDER
            }
            val reminderPendingIntent = PendingIntent.getBroadcast(
                context, 2, reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_reminder, reminderPendingIntent)

            // Quick finance button
            val financeIntent = Intent(context, ChatWidgetProvider::class.java).apply {
                action = ACTION_QUICK_FINANCE
            }
            val financePendingIntent = PendingIntent.getBroadcast(
                context, 3, financeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_finance, financePendingIntent)

            // Start floating chat button
            val floatingIntent = Intent(context, ChatWidgetProvider::class.java).apply {
                action = ACTION_START_FLOATING
            }
            val floatingPendingIntent = PendingIntent.getBroadcast(
                context, 5, floatingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start_floating, floatingPendingIntent)

            // Clear chat button
            val clearIntent = Intent(context, ChatWidgetProvider::class.java).apply {
                action = ACTION_CLEAR_CHAT
            }
            val clearPendingIntent = PendingIntent.getBroadcast(
                context, 4, clearIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_clear_button, clearPendingIntent)

            // Response area - also opens input for convenience
            views.setOnClickPendingIntent(R.id.widget_response_container, openInputPendingIntent)

            // Set default text
            views.setTextViewText(R.id.widget_response_text, "Hi! I can help with reminders and more. Tap below to chat 👇")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e("ChatWidgetProvider", "Error updating widget", e)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }
}
