package com.example.voicereminder.widget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import com.example.voicereminder.R
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ai.AIMessage
import com.example.voicereminder.data.ai.AIServiceFactory
import com.example.voicereminder.data.settings.SettingsRepository
import com.example.voicereminder.domain.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Background service that handles chat messages from the widget
 * Processes AI requests and updates the widget with responses
 */
class ChatWidgetService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var reminderManager: ReminderManager
    private val conversationHistory = mutableListOf<AIMessage>()

    companion object {
        private const val TAG = "ChatWidgetService"
        const val ACTION_SEND_MESSAGE = "com.example.voicereminder.widget.ACTION_SEND_MESSAGE"
        const val EXTRA_MESSAGE = "extra_message"

        fun sendMessage(context: Context, message: String) {
            val intent = Intent(context, ChatWidgetService::class.java).apply {
                action = ACTION_SEND_MESSAGE
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        val database = ReminderDatabase.getDatabase(applicationContext)
        reminderManager = ReminderManager(database, applicationContext)
        
        // Initialize conversation with system prompt
        serviceScope.launch {
            try {
                val settings = settingsRepository.aiSettings.first()
                conversationHistory.add(AIMessage(role = "system", content = settings.systemPrompt))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading settings", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND_MESSAGE -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return START_NOT_STICKY
                processMessage(message)
            }
        }
        return START_NOT_STICKY
    }

    private fun processMessage(userMessage: String) {
        // Update widget to show "Thinking..."
        updateWidgetStatus("Thinking...")

        serviceScope.launch {
            try {
                val settings = settingsRepository.aiSettings.first()
                
                // Add date context
                val currentDate = java.time.LocalDate.now()
                val currentDateTime = java.time.LocalDateTime.now()
                val dateContext = """
                    CURRENT DATE AND TIME:
                    Date: $currentDate
                    Day: ${currentDate.dayOfWeek}
                    Time: ${currentDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}
                """.trimIndent()

                // Update system message with date
                if (conversationHistory.isNotEmpty() && conversationHistory[0].role == "system") {
                    conversationHistory[0] = AIMessage(
                        role = "system",
                        content = settings.systemPrompt + "\n" + dateContext
                    )
                }

                // Add user message
                conversationHistory.add(AIMessage(role = "user", content = userMessage))

                // Get AI response
                val aiService = AIServiceFactory.create(settings.provider)
                val responseBuilder = StringBuilder()

                aiService.chatStream(
                    messages = conversationHistory,
                    settings = settings
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                }

                val aiResponse = responseBuilder.toString().trim()
                
                // Add to history
                conversationHistory.add(AIMessage(role = "assistant", content = aiResponse))

                // Check for tool calls and execute
                val displayResponse = if (aiResponse.contains("[TOOL:")) {
                    val (toolCommand, friendlyMessage) = parseToolResponse(aiResponse)
                    val success = executeToolCall(toolCommand)
                    if (success) friendlyMessage else "❌ Failed to execute command"
                } else {
                    aiResponse
                }

                // Update widget with response
                updateWidgetResponse(displayResponse)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
                updateWidgetResponse("Error: ${e.message ?: "Failed to get response"}")
            }
        }
    }

    private fun parseToolResponse(response: String): Pair<String, String> {
        val toolPattern = Regex("""\[TOOL:[^\]]+\]""")
        val toolMatch = toolPattern.find(response)
        val toolCommand = toolMatch?.value ?: ""
        val friendlyMessage = if (toolCommand.isNotEmpty()) {
            response.replace(toolCommand, "").trim()
        } else {
            response
        }
        return Pair(toolCommand, friendlyMessage.ifBlank { "Done! ✓" })
    }

    private suspend fun executeToolCall(toolCall: String): Boolean {
        return try {
            val toolPattern = Regex("""\[TOOL:(\w+)\|(.+)\]""")
            val match = toolPattern.find(toolCall) ?: return false

            val toolName = match.groupValues[1]
            val paramsString = match.groupValues[2]
            val params = paramsString.split("|").associate {
                val parts = it.split(":", limit = 2)
                parts[0].trim() to parts.getOrNull(1)?.trim().orEmpty()
            }

            when (toolName) {
                "create_reminder" -> {
                    val timeStr = params["time"] ?: return false
                    val message = params["message"] ?: return false
                    createReminder(timeStr, message)
                }
                "set_alarm" -> {
                    val timeStr = params["time"] ?: return false
                    val message = params["message"] ?: "Alarm"
                    setAlarm(timeStr, message)
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution failed", e)
            false
        }
    }

    private suspend fun createReminder(timeStr: String, message: String): Boolean {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val scheduledTime = java.time.LocalDateTime.parse(timeStr, formatter)
            val result = reminderManager.createReminderWithError(scheduledTime, message)
            val success = result is com.example.voicereminder.domain.ReminderResult.Success
            
            // Refresh widgets to show updated data
            if (success) {
                NextUpWidget.refresh(applicationContext)
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create reminder", e)
            false
        }
    }

    private fun setAlarm(timeStr: String, message: String): Boolean {
        return try {
            val timeParts = timeStr.split(":")
            if (timeParts.size != 2) return false
            val hour = timeParts[0].toIntOrNull() ?: return false
            val minute = timeParts[1].toIntOrNull() ?: return false

            val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm", e)
            false
        }
    }

    private fun updateWidgetStatus(status: String) {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetComponent = ComponentName(applicationContext, ChatWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

        for (widgetId in widgetIds) {
            val views = RemoteViews(packageName, R.layout.widget_chat)
            views.setTextViewText(R.id.widget_response_text, status)
            appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
        }
    }

    private fun updateWidgetResponse(response: String) {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetComponent = ComponentName(applicationContext, ChatWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

        for (widgetId in widgetIds) {
            val views = RemoteViews(packageName, R.layout.widget_chat)
            // Truncate long responses for widget display
            val displayText = if (response.length > 150) {
                response.take(147) + "..."
            } else {
                response
            }
            views.setTextViewText(R.id.widget_response_text, displayText)
            appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
