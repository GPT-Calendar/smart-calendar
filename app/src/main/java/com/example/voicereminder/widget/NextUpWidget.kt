package com.example.voicereminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.voicereminder.R
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Next Up Widget - Shows the ONE most important upcoming reminder
 * 
 * Inspired by Todoist/TickTick - focus on what's next.
 * - Shows next reminder with time
 * - Checkbox to mark complete directly
 * - Mic button to add new
 */
class NextUpWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_VOICE = "com.example.voicereminder.widget.NEXT_ADD"
        const val ACTION_COMPLETE = "com.example.voicereminder.widget.NEXT_COMPLETE"
        const val ACTION_OPEN_APP = "com.example.voicereminder.widget.NEXT_OPEN"
        const val EXTRA_REMINDER_ID = "reminder_id"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var currentReminderId: Long = -1

        /**
         * Refresh all NextUp widgets
         */
        fun refresh(context: Context) {
            val intent = Intent(context, NextUpWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val manager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, NextUpWidget::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, manager.getAppWidgetIds(component))
            }
            context.sendBroadcast(intent)
        }
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

        when (intent.action) {
            ACTION_ADD_VOICE -> {
                val voiceIntent = Intent(context, WidgetInputActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("start_voice", true)
                }
                context.startActivity(voiceIntent)
            }
            ACTION_COMPLETE -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
                if (reminderId > 0) {
                    completeReminder(context, reminderId)
                }
            }
            ACTION_OPEN_APP -> {
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(appIntent)
            }
        }
    }

    private fun completeReminder(context: Context, reminderId: Long) {
        scope.launch {
            try {
                val database = ReminderDatabase.getDatabase(context)
                val reminder = database.reminderDao().getReminderById(reminderId)
                if (reminder != null) {
                    database.reminderDao().update(reminder.copy(status = ReminderStatus.COMPLETED))
                    // Refresh widget to show next item
                    refresh(context)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_next_up)

        // Add button - voice input
        val addIntent = Intent(context, NextUpWidget::class.java).apply {
            action = ACTION_ADD_VOICE
        }
        val addPending = PendingIntent.getBroadcast(
            context, 0, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_next_add, addPending)

        // Tap widget to open app
        val openIntent = Intent(context, NextUpWidget::class.java).apply {
            action = ACTION_OPEN_APP
        }
        val openPending = PendingIntent.getBroadcast(
            context, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_next_container, openPending)
        views.setOnClickPendingIntent(R.id.widget_next_item, openPending)

        // Load next reminder
        loadNextReminder(context, appWidgetManager, appWidgetId, views)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun loadNextReminder(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews
    ) {
        scope.launch {
            try {
                val database = ReminderDatabase.getDatabase(context)
                val reminders = database.reminderDao().getUpcomingReminders(1)

                if (reminders.isNotEmpty()) {
                    val next = reminders[0]
                    currentReminderId = next.id

                    views.setTextViewText(R.id.widget_next_title, next.message)
                    
                    // Format time nicely
                    val timeText = next.scheduledTime?.let { formatTime(it) } ?: ""
                    if (timeText.isNotEmpty()) {
                        views.setTextViewText(R.id.widget_next_time, timeText)
                        views.setViewVisibility(R.id.widget_next_time, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.widget_next_time, View.GONE)
                    }

                    // Checkbox to complete
                    val completeIntent = Intent(context, NextUpWidget::class.java).apply {
                        action = ACTION_COMPLETE
                        putExtra(EXTRA_REMINDER_ID, next.id)
                    }
                    val completePending = PendingIntent.getBroadcast(
                        context, 2, completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_next_check, completePending)

                } else {
                    currentReminderId = -1
                    views.setTextViewText(R.id.widget_next_title, "All clear! 🎉")
                    views.setViewVisibility(R.id.widget_next_time, View.GONE)
                }

                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_next_title, "Tap mic to add")
                views.setViewVisibility(R.id.widget_next_time, View.GONE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        val now = LocalDateTime.now()
        
        return when {
            dateTime.toLocalDate() == now.toLocalDate() -> {
                // Today - just show time
                "Today at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            }
            dateTime.toLocalDate() == now.toLocalDate().plusDays(1) -> {
                // Tomorrow
                "Tomorrow at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            }
            ChronoUnit.DAYS.between(now.toLocalDate(), dateTime.toLocalDate()) < 7 -> {
                // This week - show day name
                dateTime.format(DateTimeFormatter.ofPattern("EEEE 'at' h:mm a"))
            }
            else -> {
                // Further out - show date
                dateTime.format(DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"))
            }
        }
    }
}
