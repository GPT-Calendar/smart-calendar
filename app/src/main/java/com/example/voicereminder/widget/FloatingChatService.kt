package com.example.voicereminder.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
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
import java.util.Locale

/**
 * Floating Chat Service - Creates a persistent chat overlay on screen
 * Users can type or speak directly without opening the app
 */
class FloatingChatService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var chatExpandedView: LinearLayout
    private lateinit var chatBubbleView: LinearLayout
    private lateinit var responseText: TextView
    private lateinit var inputField: EditText
    private lateinit var micButton: ImageButton
    private lateinit var sendButton: ImageButton

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isExpanded = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var reminderManager: ReminderManager
    private val conversationHistory = mutableListOf<AIMessage>()

    companion object {
        private const val TAG = "FloatingChatService"
        private const val CHANNEL_ID = "floating_chat_channel"
        private const val NOTIFICATION_ID = 1001

        /**
         * Check if the app has overlay permission
         */
        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        /**
         * Open settings to grant overlay permission
         */
        fun requestOverlayPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                android.widget.Toast.makeText(
                    context,
                    "Please enable 'Display over other apps' permission",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

        fun start(context: Context) {
            // Check permission first
            if (!canDrawOverlays(context)) {
                requestOverlayPermission(context)
                return
            }
            
            val intent = Intent(context, FloatingChatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingChatService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Double-check permission in case service was started without it
        if (!canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted, stopping service")
            stopSelf()
            return
        }
        
        settingsRepository = SettingsRepository(applicationContext)
        val database = ReminderDatabase.getDatabase(applicationContext)
        reminderManager = ReminderManager(database, applicationContext)

        // Initialize conversation
        serviceScope.launch {
            try {
                val settings = settingsRepository.aiSettings.first()
                conversationHistory.add(AIMessage(role = "system", content = settings.systemPrompt))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading settings", e)
            }
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        setupFloatingView()
        setupSpeechRecognizer()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Chat",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating chat active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, FloatingChatService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Chat Active")
            .setContentText("Tap the floating bubble to chat")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Use themed context to avoid attribute resolution issues
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_VirtualAssistant)
        floatingView = LayoutInflater.from(themedContext).inflate(R.layout.floating_chat, null)

        // Get views
        chatBubbleView = floatingView.findViewById(R.id.chat_bubble)
        chatExpandedView = floatingView.findViewById(R.id.chat_expanded)
        responseText = floatingView.findViewById(R.id.floating_response_text)
        inputField = floatingView.findViewById(R.id.floating_input_field)
        micButton = floatingView.findViewById(R.id.floating_mic_button)
        sendButton = floatingView.findViewById(R.id.floating_send_button)
        val closeButton = floatingView.findViewById<ImageButton>(R.id.floating_close_button)
        val minimizeButton = floatingView.findViewById<ImageButton>(R.id.floating_minimize_button)

        // Window params
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        windowManager.addView(floatingView, params)

        // Bubble click - expand
        chatBubbleView.setOnClickListener {
            expandChat(params)
        }

        // Minimize button
        minimizeButton.setOnClickListener {
            minimizeChat(params)
        }

        // Close button
        closeButton.setOnClickListener {
            stopSelf()
        }

        // Send button
        sendButton.setOnClickListener {
            val message = inputField.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                inputField.setText("")
            }
        }

        // Mic button
        micButton.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }

        // Make bubble draggable
        setupDragging(chatBubbleView, params)

        // Start minimized
        chatExpandedView.visibility = View.GONE
        chatBubbleView.visibility = View.VISIBLE
    }

    private fun expandChat(params: WindowManager.LayoutParams) {
        isExpanded = true
        chatBubbleView.visibility = View.GONE
        chatExpandedView.visibility = View.VISIBLE
        
        // Make focusable for keyboard input
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowManager.updateViewLayout(floatingView, params)
        
        // Focus input field
        inputField.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun minimizeChat(params: WindowManager.LayoutParams) {
        isExpanded = false
        chatExpandedView.visibility = View.GONE
        chatBubbleView.visibility = View.VISIBLE
        
        // Remove focus
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowManager.updateViewLayout(floatingView, params)
        
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(inputField.windowToken, 0)
    }

    private fun setupDragging(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - initialTouchX) > 10 ||
                            kotlin.math.abs(event.rawY - initialTouchY) > 10
                    if (!moved) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    micButton.setImageResource(android.R.drawable.ic_media_pause)
                    responseText.text = "🎤 Listening..."
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    micButton.setImageResource(android.R.drawable.ic_btn_speak_now)
                }

                override fun onError(error: Int) {
                    isListening = false
                    micButton.setImageResource(android.R.drawable.ic_btn_speak_now)
                    responseText.text = "Voice error. Try again."
                }

                override fun onResults(results: android.os.Bundle?) {
                    isListening = false
                    micButton.setImageResource(android.R.drawable.ic_btn_speak_now)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull() ?: ""
                    if (spokenText.isNotEmpty()) {
                        inputField.setText(spokenText)
                        sendMessage(spokenText)
                        inputField.setText("")
                    }
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull() ?: ""
                    if (partial.isNotEmpty()) {
                        inputField.setText(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (speechRecognizer == null) {
            responseText.text = "Voice not available"
            return
        }

        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        micButton.setImageResource(android.R.drawable.ic_btn_speak_now)
    }

    private fun sendMessage(message: String) {
        responseText.text = "⏳ Thinking..."

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

                if (conversationHistory.isNotEmpty() && conversationHistory[0].role == "system") {
                    conversationHistory[0] = AIMessage(
                        role = "system",
                        content = settings.systemPrompt + "\n" + dateContext
                    )
                }

                conversationHistory.add(AIMessage(role = "user", content = message))

                val aiService = AIServiceFactory.create(settings.provider)
                val responseBuilder = StringBuilder()

                aiService.chatStream(
                    messages = conversationHistory,
                    settings = settings
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                    // Update UI on main thread
                    launch(Dispatchers.Main) {
                        val currentText = responseBuilder.toString()
                        val displayText = if (currentText.contains("[TOOL:")) {
                            currentText.replace(Regex("""\[TOOL:[^\]]+\]"""), "").trim()
                                .ifEmpty { "Processing..." }
                        } else {
                            currentText
                        }
                        responseText.text = if (displayText.length > 200) {
                            displayText.take(197) + "..."
                        } else {
                            displayText
                        }
                    }
                }

                val aiResponse = responseBuilder.toString().trim()
                conversationHistory.add(AIMessage(role = "assistant", content = aiResponse))

                // Handle tool calls
                if (aiResponse.contains("[TOOL:")) {
                    val (toolCommand, friendlyMessage) = parseToolResponse(aiResponse)
                    val success = executeToolCall(toolCommand)
                    launch(Dispatchers.Main) {
                        responseText.text = if (success) friendlyMessage else "❌ Failed"
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
                launch(Dispatchers.Main) {
                    responseText.text = "Error: ${e.message?.take(50) ?: "Failed"}"
                }
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
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    val scheduledTime = java.time.LocalDateTime.parse(timeStr, formatter)
                    val result = reminderManager.createReminderWithError(scheduledTime, message)
                    result is com.example.voicereminder.domain.ReminderResult.Success
                }
                "set_alarm" -> {
                    val timeStr = params["time"] ?: return false
                    val message = params["message"] ?: "Alarm"
                    val timeParts = timeStr.split(":")
                    if (timeParts.size != 2) return false
                    val hour = timeParts[0].toIntOrNull() ?: return false
                    val minute = timeParts[1].toIntOrNull() ?: return false

                    val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                        putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
                        putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution failed", e)
            false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        serviceScope.cancel()
    }
}
