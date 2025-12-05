package com.example.voicereminder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.LocationData
import com.example.voicereminder.domain.CommandParser
import com.example.voicereminder.domain.ParseResult
import com.example.voicereminder.domain.ReminderErrorType
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.ReminderResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceInputViewModel(
    private val reminderManager: ReminderManager,
    private val onLocationPermissionNeeded: () -> Unit = {}
) : ViewModel() {
    
    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()
    
    private val _errorText = MutableStateFlow("")
    val errorText: StateFlow<String> = _errorText.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val commandParser = CommandParser()
    
    fun processVoiceInput(text: String) {
        viewModelScope.launch {
            _statusText.value = "🔄 Processing your request..."
            _errorText.value = ""
            
            // Check if this is a finance transaction command
            if (commandParser.isFinanceCommand(text)) {
                _statusText.value = "💰 Detected finance transaction..."
                processFinanceCommand(text)
                return@launch
            }
            
            // Check if this is a location-based command
            if (commandParser.isLocationCommand(text)) {
                _statusText.value = "📍 Detected location-based reminder..."
                processLocationCommand(text)
                return@launch
            }
            
            // Process as time-based reminder or alarm
            _statusText.value = "⏰ Parsing time and message..."
            val parseResult = commandParser.parseCommandWithError(text)
            
            when (parseResult) {
                is ParseResult.Error -> {
                    _errorText.value = when (parseResult.errorType) {
                        com.example.voicereminder.domain.ParseErrorType.INVALID_TIME_FORMAT -> 
                            "❌ Invalid time format.\n\n💡 Try:\n• \"Remind me at 3 PM\"\n• \"Set alarm for 7:30 AM\"\n• \"Remind me when I reach the store\"\n• \"Remind me when I get home\""
                        com.example.voicereminder.domain.ParseErrorType.PAST_TIME -> 
                            "❌ Cannot set reminder for past time.\n\n💡 Please specify a future time."
                        com.example.voicereminder.domain.ParseErrorType.NO_MESSAGE -> 
                            "❌ No message found in command.\n\n💡 Try: \"Remind me to call mom at 3 PM\""
                        com.example.voicereminder.domain.ParseErrorType.INVALID_COMMAND -> 
                            "❌ Could not understand the command.\n\n💡 Try:\n• \"Remind me to [task] at [time]\"\n• \"Set alarm for [time]\"\n• \"Remind me to [task] when I reach [place]\""
                    }
                    _statusText.value = ""
                }
                is ParseResult.Success -> {
                    val command = parseResult.command
                    
                    // Show what we're creating
                    val actionText = if (command.type == com.example.voicereminder.domain.CommandType.ALARM) {
                        "⏰ Creating alarm..."
                    } else {
                        "📝 Creating reminder..."
                    }
                    _statusText.value = actionText
                    
                    // Handle alarm vs reminder based on command type
                    val result = if (command.type == com.example.voicereminder.domain.CommandType.ALARM) {
                        reminderManager.createAlarm(command.scheduledTime, command.message)
                    } else {
                        reminderManager.createReminderWithError(
                            command.scheduledTime,
                            command.message
                        )
                    }
                    
                    when (result) {
                        is ReminderResult.Success -> {
                            val timeStr = formatTimeForDisplay(command.scheduledTime)
                            val dateStr = formatDateForDisplay(command.scheduledTime)
                            
                            if (command.type == com.example.voicereminder.domain.CommandType.ALARM) {
                                _statusText.value = "✅ Alarm set successfully!\n\n⏰ Time: $timeStr\n📅 Date: $dateStr\n\nYou can view it in the Calendar tab."
                            } else {
                                _statusText.value = "✅ Reminder created successfully!\n\n📝 Message: ${command.message}\n⏰ Time: $timeStr\n📅 Date: $dateStr\n\nYou can view it in the Calendar tab."
                            }
                        }
                        is ReminderResult.Error -> {
                            _errorText.value = when (result.errorType) {
                                ReminderErrorType.DATABASE_ERROR -> 
                                    "❌ Database error occurred.\n\n💡 Please try again. If the problem persists, restart the app."
                                ReminderErrorType.SCHEDULING_ERROR -> 
                                    "❌ Scheduling error occurred.\n\n💡 Please check that:\n• Alarm permissions are granted\n• Exact alarm permission is enabled\n• Battery optimization is disabled for this app"
                                ReminderErrorType.INVALID_INPUT -> 
                                    "❌ ${result.message}\n\n💡 Please check your input and try again."
                                ReminderErrorType.UNKNOWN_ERROR -> 
                                    "❌ An unexpected error occurred.\n\n💡 Please try again or restart the app."
                            }
                            _statusText.value = ""
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Process a finance transaction voice command
     */
    private suspend fun processFinanceCommand(text: String) {
        _statusText.value = "💰 Parsing transaction details..."
        
        // Parse the finance command
        val financeCommand = commandParser.parseFinanceCommand(text)
        
        if (financeCommand == null) {
            _errorText.value = "❌ Could not understand the transaction.\n\n💡 Try:\n• \"I spent 500 birr on groceries\"\n• \"I received 2000 birr salary\"\n• \"I bought coffee for 50 birr\""
            _statusText.value = ""
            return
        }
        
        _statusText.value = "💾 Saving transaction..."
        
        try {
            // Save finance transaction
            reminderManager.saveFinanceTransaction(
                amount = financeCommand.amount,
                currency = financeCommand.currency,
                description = financeCommand.description,
                isIncome = financeCommand.transactionType == "CREDIT"
            )
            
            val typeEmoji = if (financeCommand.transactionType == "CREDIT") "📈" else "📉"
            val typeText = if (financeCommand.transactionType == "CREDIT") "Income" else "Expense"
            
            _statusText.value = "✅ Transaction saved successfully!\n\n" +
                    "$typeEmoji Type: $typeText\n" +
                    "💵 Amount: ${financeCommand.amount} ${financeCommand.currency}\n" +
                    "📝 Description: ${financeCommand.description}\n\n" +
                    "View it in the Finance tab."
        } catch (e: Exception) {
            android.util.Log.e("VoiceInputViewModel", "Error saving finance transaction", e)
            _errorText.value = "❌ Failed to save transaction: ${e.message}"
            _statusText.value = ""
        }
    }
    
    /**
     * Process a location-based voice command
     */
    private suspend fun processLocationCommand(text: String) {
        // Trigger location permission check
        onLocationPermissionNeeded()
        
        _statusText.value = "📍 Parsing location details..."
        
        // Parse the location command
        val locationCommand = commandParser.parseLocationCommand(text)
        
        if (locationCommand == null) {
            _errorText.value = "❌ Could not understand the location command.\n\n💡 Try:\n• \"Remind me to buy milk at the store\"\n• \"Remind me to call John when I get home\"\n• \"Remind me to pick up package at the post office\""
            _statusText.value = ""
            return
        }
        
        _statusText.value = "📝 Creating location reminder..."
        
        // Create LocationData from parsed command
        val locationData = LocationData(
            locationType = locationCommand.locationType,
            latitude = locationCommand.latitude,
            longitude = locationCommand.longitude,
            placeName = locationCommand.placeName,
            placeCategory = locationCommand.placeCategory,
            radius = if (locationCommand.locationType == com.example.voicereminder.data.LocationType.GENERIC_CATEGORY) 200f else 100f
        )
        
        // Create location reminder
        val result = reminderManager.createLocationReminder(
            message = locationCommand.message,
            locationData = locationData
        )
        
        when (result) {
            is ReminderResult.Success -> {
                val locationDescription = when {
                    locationCommand.placeName != null -> locationCommand.placeName
                    locationCommand.placeCategory != null -> "any ${locationCommand.placeCategory.name.lowercase().replace('_', ' ')}"
                    else -> "the location"
                }
                _statusText.value = "✅ Location reminder created successfully!\n\n📝 Message: ${locationCommand.message}\n📍 Location: $locationDescription\n\nYou'll be notified when you arrive at this location."
            }
            is ReminderResult.Error -> {
                _errorText.value = when (result.errorType) {
                    ReminderErrorType.DATABASE_ERROR -> 
                        "❌ Database error occurred.\n\n💡 Please try again. If the problem persists, restart the app."
                    ReminderErrorType.SCHEDULING_ERROR -> 
                        "❌ ${result.message}\n\n💡 Please check that:\n• Location permissions are granted\n• Location services are enabled\n• Background location is allowed"
                    ReminderErrorType.INVALID_INPUT -> 
                        "❌ ${result.message}\n\n💡 Please check your input and try again."
                    ReminderErrorType.UNKNOWN_ERROR -> 
                        "❌ ${result.message}\n\n💡 Please try again or restart the app."
                }
                _statusText.value = ""
            }
        }
    }
    
    fun updateStatus(text: String) {
        _statusText.value = text
    }
    
    fun updateError(text: String) {
        _errorText.value = text
    }
    
    fun updateListeningState(isListening: Boolean) {
        _isListening.value = isListening
    }
    
    fun clearError() {
        _errorText.value = ""
    }
    
    private fun formatTimeForDisplay(time: java.time.LocalDateTime): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        return time.format(formatter)
    }
    
    private fun formatDateForDisplay(time: java.time.LocalDateTime): String {
        val today = java.time.LocalDate.now()
        val tomorrow = today.plusDays(1)
        val date = time.toLocalDate()
        
        return when {
            date == today -> "Today"
            date == tomorrow -> "Tomorrow"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
                time.format(formatter)
            }
        }
    }
}