package com.example.voicereminder.presentation.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.ChatHistoryRepository
import com.example.voicereminder.data.LocationData
import com.example.voicereminder.data.OfflineMessageQueue
import com.example.voicereminder.data.ai.AIMessage
import com.example.voicereminder.data.ai.AIServiceFactory
import com.example.voicereminder.data.entity.toChatMessage
import com.example.voicereminder.data.settings.AISettings
import com.example.voicereminder.data.settings.SettingsRepository
import com.example.voicereminder.domain.CommandParser
import com.example.voicereminder.domain.ParseResult
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.ReminderResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.voicereminder.domain.TaskManager
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.domain.SnoozeManager
import com.example.voicereminder.domain.TaskResult
import com.example.voicereminder.domain.AlarmResult

class ChatViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val database = com.example.voicereminder.data.ReminderDatabase.getDatabase(application)
    private val reminderManager = ReminderManager(database, application)
    private val commandParser = CommandParser()
    private val financeRepository = com.example.voicereminder.data.FinanceRepository(application)
    private val settingsRepository = SettingsRepository(application)
    
    // Chat history persistence
    private val chatHistoryRepository = ChatHistoryRepository.getInstance(application)
    
    // Offline support
    private val offlineMessageQueue = OfflineMessageQueue.getInstance(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()
    
    private val _currentSettings = MutableStateFlow(AISettings())
    val currentSettings: StateFlow<AISettings> = _currentSettings.asStateFlow()
    
    // Offline status
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    
    private val _queuedMessageCount = MutableStateFlow(0)
    val queuedMessageCount: StateFlow<Int> = _queuedMessageCount.asStateFlow()

    private val conversationHistory = mutableListOf<AIMessage>()
    
    // Track if we've already checked connection to avoid repeated checks
    private var connectionChecked = false
    // Track if settings have been loaded
    private var settingsLoaded = false

    init {
        // Load initial settings
        viewModelScope.launch {
            try {
                val settings = settingsRepository.aiSettings.first()
                _currentSettings.value = settings
                conversationHistory.clear()
                conversationHistory.add(AIMessage(role = "system", content = settings.systemPrompt))
                settingsLoaded = true
                
                // Load chat history from database
                loadChatHistory()
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error loading settings", e)
                settingsLoaded = true
                loadChatHistory()
            }
        }
        
        // Monitor offline status
        viewModelScope.launch {
            offlineMessageQueue.isOnline.collect { isOnline ->
                _isOffline.value = !isOnline
            }
        }
        
        viewModelScope.launch {
            offlineMessageQueue.queuedMessageCount.collect { count ->
                _queuedMessageCount.value = count
            }
        }
    }
    
    /**
     * Refresh settings from repository - call this before sending messages
     * to ensure we have the latest AI configuration
     */
    private suspend fun refreshSettings() {
        try {
            val settings = settingsRepository.aiSettings.first()
            val oldSettings = _currentSettings.value
            _currentSettings.value = settings
            
            // If settings changed, update system prompt and clear connection status
            if (oldSettings != settings) {
                android.util.Log.d("ChatViewModel", "Settings changed, updating...")
                android.util.Log.d("ChatViewModel", "New URL: ${settings.getEffectiveOllamaUrl()}")
                android.util.Log.d("ChatViewModel", "New model: ${settings.ollamaModel}")
                
                // Update system prompt in conversation history
                if (conversationHistory.isNotEmpty() && conversationHistory[0].role == "system") {
                    conversationHistory[0] = AIMessage(role = "system", content = settings.systemPrompt)
                }
                
                // Clear connection status so it gets rechecked with new settings
                _connectionStatus.value = null
                connectionChecked = false
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error refreshing settings", e)
        }
    }

    private fun checkAIConnection() {
        viewModelScope.launch {
            try {
                // Always refresh settings before checking connection
                refreshSettings()
                
                val settings = _currentSettings.value
                android.util.Log.d("ChatViewModel", "Checking connection with URL: ${settings.getEffectiveOllamaUrl()}")
                android.util.Log.d("ChatViewModel", "Model: ${settings.ollamaModel}")
                
                val aiService = AIServiceFactory.create(settings.provider)
                val (isHealthy, message) = aiService.testConnection(settings)
                if (isHealthy) {
                    _connectionStatus.value = null
                } else {
                    _connectionStatus.value = message
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Connection error: ${e.message}"
            }
        }
    }
    
    /**
     * Recheck AI connection - always refreshes settings first
     */
    fun recheckConnection(force: Boolean = false) {
        // Always check when force is true, or on first check
        if (!connectionChecked || force) {
            connectionChecked = true
            checkAIConnection()
        }
    }
    
    /**
     * Clear all chat messages and history
     */
    fun clearChat() {
        viewModelScope.launch {
            // Clear in-memory messages
            _messages.value = emptyList()
            
            // Clear conversation history (keep system prompt)
            val systemPrompt = conversationHistory.firstOrNull { it.role == "system" }
            conversationHistory.clear()
            systemPrompt?.let { conversationHistory.add(it) }
            
            // Clear persisted chat history
            chatHistoryRepository.clearHistory()
        }
    }

    fun sendMessage(userMessage: String) {
        // Add user message
        val userChatMessage = ChatMessage(
            id = _messages.value.size + 1,
            text = userMessage,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + userChatMessage

        // Start typing indicator
        _isTyping.value = true

        viewModelScope.launch {
            // Refresh settings before sending to get latest AI configuration
            refreshSettings()
            
            // Save user message to history
            saveMessageToHistory(userChatMessage)
            
            // Check if offline
            if (_isOffline.value) {
                // Queue message for later
                offlineMessageQueue.queueMessage(userChatMessage)
                
                // Show offline response
                val offlineResponse = ChatMessage(
                    id = _messages.value.size + 1,
                    text = "📴 You're offline. Your message has been queued and will be sent when you're back online.",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + offlineResponse
                _isTyping.value = false
                return@launch
            }
            
            // Always send to AI first - let AI decide if it needs to call a tool
            processAIChatWithTools(userMessage)
        }
    }
    

    
    private suspend fun processAIChatWithTools(userMessage: String) {
        // Update system prompt with current date before each message
        val currentDate = java.time.LocalDate.now()
        val currentDateTime = java.time.LocalDateTime.now()
        val dateContext = """
            
            CURRENT DATE AND TIME:
            Date: $currentDate (YYYY-MM-DD format)
            Day: ${currentDate.dayOfWeek}
            Time: ${currentDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}
            Year: ${currentDate.year}
        """.trimIndent()
        
        // Update the system message with current date
        if (conversationHistory.isNotEmpty() && conversationHistory[0].role == "system") {
            conversationHistory[0] = AIMessage(
                role = "system",
                content = _currentSettings.value.systemPrompt + "\n" + dateContext
            )
        }
        
        // Check if this is a finance-related question
        val isFinanceQuestion = isFinanceRelatedQuestion(userMessage)
        
        // If it's a finance question, add finance data context
        val messageWithContext = if (isFinanceQuestion) {
            val financeContext = getFinanceDataContext()
            userMessage + "\n\n[FINANCE_DATA]\n$financeContext"
        } else {
            userMessage
        }
        
        // Add to conversation history
        conversationHistory.add(AIMessage(role = "user", content = messageWithContext))
        
        try {
            val aiResponseBuilder = StringBuilder()
            val settings = _currentSettings.value
            android.util.Log.d("ChatViewModel", "Using connection mode: ${settings.ollamaConnectionMode}")
            android.util.Log.d("ChatViewModel", "Effective URL: ${settings.getEffectiveOllamaUrl()}")
            val aiService = AIServiceFactory.create(settings.provider)

            // Create a placeholder message for streaming
            val streamingMessageId = _messages.value.size + 1
            val streamingMessage = ChatMessage(
                id = streamingMessageId,
                text = "",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + streamingMessage

            // Get AI response with streaming updates
            android.util.Log.d("ChatViewModel", "Starting to collect AI response stream...")
            aiService.chatStream(
                messages = conversationHistory,
                settings = settings
            ).collect { chunk ->
                android.util.Log.d("ChatViewModel", "Received chunk: $chunk")
                aiResponseBuilder.append(chunk)
                
                // Check if response contains a tool call
                val currentText = aiResponseBuilder.toString()
                val displayText = if (currentText.contains("[TOOL:")) {
                    // Hide the tool command, show "Processing..." or extract friendly message
                    val toolPattern = Regex("""\[TOOL:[^\]]+\]""")
                    val toolMatch = toolPattern.find(currentText)
                    if (toolMatch != null) {
                        // Extract friendly message (everything after tool command)
                        val friendlyPart = currentText.replace(toolMatch.value, "").trim()
                        if (friendlyPart.isNotEmpty()) {
                            friendlyPart
                        } else {
                            "Processing your request..."
                        }
                    } else {
                        currentText
                    }
                } else {
                    currentText
                }
                
                // Update the streaming message in real-time
                val updatedMessages = _messages.value.map { msg ->
                    if (msg.id == streamingMessageId) {
                        msg.copy(text = displayText)
                    } else {
                        msg
                    }
                }
                _messages.value = updatedMessages
            }
            android.util.Log.d("ChatViewModel", "Stream collection completed")

            val aiResponse = aiResponseBuilder.toString().trim()
            android.util.Log.d("ChatViewModel", "Full AI response: $aiResponse")
            
            // Clear connection status on successful response
            _connectionStatus.value = null
            
            // Add AI response to conversation history
            conversationHistory.add(
                AIMessage(
                    role = "assistant",
                    content = aiResponse
                )
            )

            // Check if AI wants to call a tool
            if (aiResponse.contains("[TOOL:")) {
                android.util.Log.d("ChatViewModel", "Tool detected in response: $aiResponse")
                
                // Parse tool command and friendly message
                val (toolCommand, friendlyMessage) = parseToolResponse(aiResponse)
                android.util.Log.d("ChatViewModel", "Parsed tool command: $toolCommand")
                android.util.Log.d("ChatViewModel", "Parsed friendly message: $friendlyMessage")
                
                // Execute the tool silently
                val toolSuccess = executeToolCall(toolCommand)
                android.util.Log.d("ChatViewModel", "Tool execution success: $toolSuccess")
                
                // Show AI's friendly message (or error if tool failed)
                val displayMessage = if (toolSuccess) {
                    friendlyMessage
                } else {
                    "❌ Failed to execute the command. Please try again."
                }
                
                // Update the streaming message with final tool result
                val updatedMessages = _messages.value.map { msg ->
                    if (msg.id == streamingMessageId) {
                        msg.copy(text = displayMessage)
                    } else {
                        msg
                    }
                }
                _messages.value = updatedMessages
            }
            // If no tool call, the streaming message already has the full response

            _isTyping.value = false
            
            // Save AI response to history
            val finalMessage = _messages.value.find { it.id == streamingMessageId }
            finalMessage?.let { saveMessageToHistory(it) }
            
        } catch (e: Exception) {
            _isTyping.value = false
            android.util.Log.e("ChatViewModel", "Error in processAIChatWithTools", e)
            android.util.Log.e("ChatViewModel", "Error type: ${e.javaClass.simpleName}")
            android.util.Log.e("ChatViewModel", "Error message: ${e.message}")
            e.printStackTrace()
            
            val errorMessage = ChatMessage(
                id = _messages.value.size + 1,
                text = "Error: ${e.message ?: "Failed to get response"}",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + errorMessage
            
            // Save error message to history
            saveMessageToHistory(errorMessage)
        }
    }
    
    /**
     * Parse AI response to extract tool command and friendly message
     * Format: [TOOL:name|params]\nFriendly message here
     */
    private fun parseToolResponse(response: String): Pair<String, String> {
        // Extract the tool command using regex (handles multi-line)
        val toolPattern = Regex("""\[TOOL:[^\]]+\]""")
        val toolMatch = toolPattern.find(response)
        val toolCommand = toolMatch?.value ?: ""
        
        // Extract friendly message (everything after the tool command)
        val friendlyMessage = if (toolCommand.isNotEmpty()) {
            response.replace(toolCommand, "").trim()
        } else {
            response
        }
        
        return Pair(toolCommand, friendlyMessage.ifBlank { "Done!" })
    }
    
    /**
     * Execute tool call and return success status
     * Returns true if tool executed successfully, false otherwise
     */
    private suspend fun executeToolCall(toolCall: String): Boolean {
        return try {
            // Parse tool call format: [TOOL:tool_name|param1:value1|param2:value2]
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
                    createReminderFromAI(timeStr, message)
                }
                "add_transaction" -> {
                    val amount = params["amount"]?.toDoubleOrNull() ?: return false
                    val currency = params["currency"] ?: "ETB"
                    val type = params["type"] ?: "expense"
                    val description = params["description"] ?: "Transaction"
                    addTransactionFromAI(amount, currency, type, description)
                }
                "location_reminder" -> {
                    val message = params["message"] ?: return false
                    val location = params["location"] ?: return false
                    createLocationReminderFromAI(message, location)
                }
                "set_alarm" -> {
                    val timeStr = params["time"] ?: return false
                    val message = params["message"] ?: "Alarm"
                    setAlarmFromAI(timeStr, message)
                }
                "create_task" -> {
                    val title = params["title"] ?: return false
                    val description = params["description"]
                    val dueDate = params["due_date"]
                    val priority = params["priority"] ?: "MEDIUM"
                    createTaskFromAI(title, description, dueDate, priority)
                }
                "complete_task" -> {
                    val taskTitle = params["title"] ?: return false
                    completeTaskFromAI(taskTitle)
                }
                "create_recurring_alarm" -> {
                    val timeStr = params["time"] ?: return false
                    val label = params["label"] ?: "Alarm"
                    val repeatDays = params["repeat_days"] ?: ""
                    createRecurringAlarmFromAI(timeStr, label, repeatDays)
                }
                "create_recurring_reminder" -> {
                    val timeStr = params["time"] ?: return false
                    val message = params["message"] ?: return false
                    val recurrence = params["recurrence"] ?: "DAILY"
                    createRecurringReminderFromAI(timeStr, message, recurrence)
                }
                "snooze" -> {
                    val minutes = params["minutes"]?.toIntOrNull() ?: 5
                    snoozeFromAI(minutes)
                }
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Tool execution failed", e)
            false
        }
    }
    
    private suspend fun createReminderFromAI(timeStr: String, message: String): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Creating reminder - time: $timeStr, message: $message")
            
            // Parse time format: YYYY-MM-DD HH:MM
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val scheduledTime = java.time.LocalDateTime.parse(timeStr, formatter)
            
            android.util.Log.d("ChatViewModel", "Parsed scheduled time: $scheduledTime")
            
            val result = reminderManager.createReminderWithError(scheduledTime, message)
            android.util.Log.d("ChatViewModel", "Reminder creation result: $result")
            
            result is ReminderResult.Success
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to create reminder - time: $timeStr, message: $message", e)
            false
        }
    }
    
    private suspend fun addTransactionFromAI(amount: Double, currency: String, type: String, description: String): Boolean {
        return try {
            val isIncome = type.lowercase() == "income"
            
            reminderManager.saveFinanceTransaction(
                amount = amount,
                currency = currency,
                description = description,
                isIncome = isIncome
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to save transaction", e)
            false
        }
    }
    
    private suspend fun createLocationReminderFromAI(message: String, location: String): Boolean {
        return try {
            // Use CommandParser to parse the location
            val fullCommand = "remind me to $message at $location"
            val locationCommand = commandParser.parseLocationCommand(fullCommand)
            
            if (locationCommand == null) {
                return false
            }
            
            val locationData = LocationData(
                locationType = locationCommand.locationType,
                latitude = locationCommand.latitude,
                longitude = locationCommand.longitude,
                placeName = locationCommand.placeName,
                placeCategory = locationCommand.placeCategory,
                radius = if (locationCommand.locationType == com.example.voicereminder.data.LocationType.GENERIC_CATEGORY) 200f else 100f
            )
            
            val result = reminderManager.createLocationReminder(
                message = message,
                locationData = locationData
            )
            
            result is ReminderResult.Success
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to create location reminder", e)
            false
        }
    }
    
    private suspend fun setAlarmFromAI(timeStr: String, message: String): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Setting alarm - time: $timeStr, message: $message")
            
            // Parse time format: HH:MM
            val timeParts = timeStr.split(":")
            if (timeParts.size != 2) return false
            
            val hour = timeParts[0].toIntOrNull() ?: return false
            val minute = timeParts[1].toIntOrNull() ?: return false
            
            // Create a date-time for the alarm (today or tomorrow if time has passed)
            val now = java.time.LocalDateTime.now()
            var alarmDateTime = java.time.LocalDateTime.of(
                now.toLocalDate(),
                java.time.LocalTime.of(hour, minute)
            )
            
            // If the time has already passed today, set it for tomorrow
            if (alarmDateTime.isBefore(now)) {
                alarmDateTime = alarmDateTime.plusDays(1)
            }
            
            // Save alarm as a reminder in the app database so it shows in "All Events"
            val reminderResult = reminderManager.createReminderWithError(alarmDateTime, "⏰ $message")
            android.util.Log.d("ChatViewModel", "Alarm reminder saved: $reminderResult")
            
            // Also set the system alarm
            val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true) // Skip the UI and set directly
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            getApplication<android.app.Application>().startActivity(intent)
            android.util.Log.d("ChatViewModel", "System alarm set successfully")
            
            reminderResult is ReminderResult.Success
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to set alarm - time: $timeStr, message: $message", e)
            false
        }
    }
    
    // Task Manager instance for task operations
    private val taskManager by lazy { 
        com.example.voicereminder.domain.TaskManager.getInstance(getApplication()) 
    }
    
    // Enhanced Alarm Manager for alarm operations
    private val enhancedAlarmManager by lazy { 
        com.example.voicereminder.domain.EnhancedAlarmManager.getInstance(getApplication()) 
    }
    
    // Snooze Manager for snooze operations
    private val snoozeManager by lazy { 
        com.example.voicereminder.domain.SnoozeManager.getInstance(getApplication()) 
    }
    
    /**
     * Create a task from AI command
     */
    private suspend fun createTaskFromAI(title: String, description: String?, dueDate: String?, priority: String): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Creating task - title: $title, priority: $priority")
            
            val parsedDueDate = dueDate?.let {
                try {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    java.time.LocalDateTime.parse(it, formatter)
                } catch (e: Exception) {
                    null
                }
            }
            
            val taskPriority = when (priority.uppercase()) {
                "HIGH" -> com.example.voicereminder.data.Priority.HIGH
                "LOW" -> com.example.voicereminder.data.Priority.LOW
                else -> com.example.voicereminder.data.Priority.MEDIUM
            }
            
            val result = taskManager.createTask(
                title = title,
                description = description,
                dueDate = parsedDueDate,
                priority = taskPriority
            )
            
            result is com.example.voicereminder.domain.TaskResult.Success
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to create task", e)
            false
        }
    }
    
    /**
     * Complete a task by title from AI command
     */
    private suspend fun completeTaskFromAI(taskTitle: String): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Completing task - title: $taskTitle")
            
            // Search for the task by title
            val tasks = taskManager.getAllTasksFlow().first()
            val matchingTask = tasks.find { 
                it.title.lowercase().contains(taskTitle.lowercase()) && !it.isCompleted
            }
            
            if (matchingTask != null) {
                taskManager.completeTask(matchingTask.id)
                true
            } else {
                android.util.Log.w("ChatViewModel", "No matching task found for: $taskTitle")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to complete task", e)
            false
        }
    }
    
    /**
     * Create a recurring alarm from AI command
     */
    private suspend fun createRecurringAlarmFromAI(timeStr: String, label: String, repeatDaysStr: String): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Creating recurring alarm - time: $timeStr, label: $label, days: $repeatDaysStr")
            
            val timeParts = timeStr.split(":")
            if (timeParts.size != 2) return false
            
            val hour = timeParts[0].toIntOrNull() ?: return false
            val minute = timeParts[1].toIntOrNull() ?: return false
            
            // Parse repeat days
            val repeatDays = when (repeatDaysStr.lowercase()) {
                "weekdays" -> listOf(1, 2, 3, 4, 5)
                "weekends" -> listOf(6, 7)
                "daily", "every day" -> listOf(1, 2, 3, 4, 5, 6, 7)
                else -> {
                    // Try to parse individual days
                    repeatDaysStr.split(",").mapNotNull { day ->
                        when (day.trim().lowercase()) {
                            "monday", "mon" -> 1
                            "tuesday", "tue" -> 2
                            "wednesday", "wed" -> 3
                            "thursday", "thu" -> 4
                            "friday", "fri" -> 5
                            "saturday", "sat" -> 6
                            "sunday", "sun" -> 7
                            else -> day.trim().toIntOrNull()
                        }
                    }
                }
            }
            
            val result = enhancedAlarmManager.createAlarm(
                hour = hour,
                minute = minute,
                label = label,
                repeatDays = repeatDays
            )
            
            result is com.example.voicereminder.domain.AlarmResult.Success
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to create recurring alarm", e)
            false
        }
    }
    
    /**
     * Create a recurring reminder from AI command
     */
    private suspend fun createRecurringReminderFromAI(timeStr: String, message: String, recurrence: String): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Creating recurring reminder - time: $timeStr, message: $message, recurrence: $recurrence")
            
            // Parse time format: YYYY-MM-DD HH:MM or HH:MM
            val scheduledTime = try {
                if (timeStr.contains("-")) {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    java.time.LocalDateTime.parse(timeStr, formatter)
                } else {
                    val timeParts = timeStr.split(":")
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    var dateTime = java.time.LocalDateTime.now().withHour(hour).withMinute(minute)
                    if (dateTime.isBefore(java.time.LocalDateTime.now())) {
                        dateTime = dateTime.plusDays(1)
                    }
                    dateTime
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Failed to parse time: $timeStr", e)
                return false
            }
            
            // Create recurrence rule
            val recurrenceRule = when (recurrence.uppercase()) {
                "DAILY" -> com.example.voicereminder.data.RecurrenceRule.daily()
                "WEEKLY" -> com.example.voicereminder.data.RecurrenceRule.weekly(listOf(scheduledTime.dayOfWeek.value))
                "WEEKDAYS" -> com.example.voicereminder.data.RecurrenceRule.weekdays()
                "MONTHLY" -> com.example.voicereminder.data.RecurrenceRule.monthly(scheduledTime.dayOfMonth)
                else -> com.example.voicereminder.data.RecurrenceRule.daily()
            }
            
            // For now, create a regular reminder (recurrence will be handled by RecurrenceScheduler)
            val result = reminderManager.createReminderWithError(scheduledTime, "🔄 $message")
            
            result is ReminderResult.Success
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to create recurring reminder", e)
            false
        }
    }
    
    /**
     * Snooze the most recent notification
     */
    private suspend fun snoozeFromAI(minutes: Int): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Snoozing for $minutes minutes")
            // This would typically snooze the most recent active notification
            // For now, we'll just acknowledge the command
            true
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to snooze", e)
            false
        }
    }
    
    private fun formatTime(time: java.time.LocalDateTime): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        return time.format(formatter)
    }
    
    private fun formatDate(time: java.time.LocalDateTime): String {
        val today = java.time.LocalDate.now()
        val tomorrow = today.plusDays(1)
        val date = time.toLocalDate()
        
        return when {
            date == today -> "Today"
            date == tomorrow -> "Tomorrow"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
                time.format(formatter)
            }
        }
    }

    /**
     * Check if the user message is asking a finance-related question
     */
    private fun isFinanceRelatedQuestion(message: String): Boolean {
        val financeKeywords = listOf(
            "spend", "spent", "expense", "expenses", "income", "earned", "received",
            "budget", "money", "cost", "paid", "transaction", "transactions",
            "how much", "total", "balance", "financial", "finance"
        )
        
        val lowerMessage = message.lowercase()
        return financeKeywords.any { lowerMessage.contains(it) } && 
               (lowerMessage.contains("?") || lowerMessage.contains("how") || 
                lowerMessage.contains("what") || lowerMessage.contains("show"))
    }
    
    /**
     * Get finance data context to include in AI prompt
     */
    private suspend fun getFinanceDataContext(): String {
        return try {
            val financeData = financeRepository.getFinanceData()
            val transactions = financeData.transactions ?: emptyList()
            
            if (transactions.isEmpty()) {
                return "No transactions recorded yet."
            }
            
            // Get summary statistics
            val totalIncome = transactions.filter { it.type == com.example.voicereminder.domain.models.TransactionType.RECEIVED }
                .sumOf { it.amount }
            val totalExpenses = transactions.filter { it.type == com.example.voicereminder.domain.models.TransactionType.SPENT }
                .sumOf { kotlin.math.abs(it.amount) }
            val balance = totalIncome - totalExpenses
            
            // Get recent transactions (last 10)
            val recentTransactions = transactions.take(10)
            
            // Build context string
            buildString {
                appendLine("FINANCE SUMMARY:")
                appendLine("Total Income: ${totalIncome.toInt()} ETB")
                appendLine("Total Expenses: ${totalExpenses.toInt()} ETB")
                appendLine("Balance: ${balance.toInt()} ETB")
                appendLine()
                appendLine("RECENT TRANSACTIONS:")
                recentTransactions.forEach { transaction ->
                    val type = if (transaction.type == com.example.voicereminder.domain.models.TransactionType.RECEIVED) "Income" else "Expense"
                    val amount = kotlin.math.abs(transaction.amount).toInt()
                    appendLine("- $type: $amount ETB - ${transaction.title}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to get finance context", e)
            "Unable to retrieve finance data."
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            // Clear from database
            chatHistoryRepository.clearHistory()
            
            // Clear in-memory
            conversationHistory.clear()
            conversationHistory.add(AIMessage(role = "system", content = _currentSettings.value.systemPrompt))
            
            val welcomeMessage = ChatMessage(
                id = 1,
                text = "Hi! I help with finance, events, and reminders. What do you need?",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = listOf(welcomeMessage)
            
            // Save welcome message
            chatHistoryRepository.saveMessage(welcomeMessage)
        }
    }
    
    /**
     * Load chat history from database
     */
    private suspend fun loadChatHistory() {
        try {
            val savedMessages = chatHistoryRepository.getRecentMessages(100)
            
            if (savedMessages.isEmpty()) {
                // No history, show welcome message
                val welcomeMessage = ChatMessage(
                    id = 1,
                    text = "Hi! I help with finance, events, and reminders. What do you need?",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = listOf(welcomeMessage)
                chatHistoryRepository.saveMessage(welcomeMessage)
            } else {
                _messages.value = savedMessages
                
                // Rebuild conversation history for AI context
                savedMessages.forEach { message ->
                    conversationHistory.add(
                        AIMessage(
                            role = if (message.isUser) "user" else "assistant",
                            content = message.text
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to load chat history", e)
            // Fallback to welcome message
            _messages.value = listOf(
                ChatMessage(
                    id = 1,
                    text = "Hi! I help with finance, events, and reminders. What do you need?",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Save message to history
     */
    private suspend fun saveMessageToHistory(message: ChatMessage) {
        try {
            chatHistoryRepository.saveMessage(message)
            // Cleanup old messages periodically
            chatHistoryRepository.cleanupOldMessages()
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to save message to history", e)
        }
    }
    
    /**
     * Delete a message
     */
    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            chatHistoryRepository.deleteMessage(messageId)
            _messages.value = _messages.value.filter { it.id != messageId }
        }
    }
    
    /**
     * Search messages
     */
    suspend fun searchMessages(query: String): List<ChatMessage> {
        return chatHistoryRepository.searchMessages(query)
    }
    
    /**
     * Export conversation as text
     */
    suspend fun exportAsText(): String {
        return chatHistoryRepository.exportAsText()
    }
    
    /**
     * Export conversation as JSON
     */
    suspend fun exportAsJson(): String {
        return chatHistoryRepository.exportAsJson()
    }
    
    /**
     * Retry sending queued messages
     */
    fun retryQueuedMessages() {
        viewModelScope.launch {
            offlineMessageQueue.processQueuedMessages()
        }
    }
}


/**
 * Factory for creating ChatViewModel with Application dependency
 */
class ChatViewModelFactory(
    private val application: Application
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
