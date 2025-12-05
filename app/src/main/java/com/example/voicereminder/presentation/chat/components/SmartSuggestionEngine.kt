package com.example.voicereminder.presentation.chat.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Smart suggestion data class
 */
data class SmartSuggestion(
    val label: String,
    val command: String,
    val icon: ImageVector? = null,
    val priority: Int = 0
)

/**
 * Context for generating suggestions
 */
data class SuggestionContext(
    val timeOfDay: TimeOfDay,
    val hasOverdueTasks: Boolean = false,
    val hasUpcomingReminders: Boolean = false,
    val hasPendingBudgetAlerts: Boolean = false,
    val lastAction: LastAction? = null,
    val recentCommands: List<String> = emptyList()
)

enum class TimeOfDay {
    MORNING,    // 5 AM - 12 PM
    AFTERNOON,  // 12 PM - 5 PM
    EVENING,    // 5 PM - 9 PM
    NIGHT       // 9 PM - 5 AM
}

sealed class LastAction {
    object CreatedReminder : LastAction()
    object CreatedTask : LastAction()
    object SetAlarm : LastAction()
    object AddedTransaction : LastAction()
    object ViewedSchedule : LastAction()
    object ViewedFinance : LastAction()
}

/**
 * Engine for generating smart, context-aware suggestions
 */
class SmartSuggestionEngine {
    
    companion object {
        private val MORNING_SUGGESTIONS = listOf(
            SmartSuggestion("☀️ Morning summary", "What's my schedule for today?", Icons.Default.WbSunny, 10),
            SmartSuggestion("📅 Today's events", "Show today's reminders", Icons.Default.Event, 9),
            SmartSuggestion("✅ Pending tasks", "Show my tasks", Icons.Default.CheckCircle, 8)
        )
        
        private val AFTERNOON_SUGGESTIONS = listOf(
            SmartSuggestion("📊 Progress check", "How many tasks did I complete?", Icons.Default.Assessment, 10),
            SmartSuggestion("💰 Spending today", "How much did I spend today?", Icons.Default.AttachMoney, 9),
            SmartSuggestion("⏰ Set reminder", "Remind me to ", Icons.Default.Notifications, 8)
        )
        
        private val EVENING_SUGGESTIONS = listOf(
            SmartSuggestion("💰 Today's expenses", "Show today's transactions", Icons.Default.Receipt, 10),
            SmartSuggestion("📋 Review tasks", "Show incomplete tasks", Icons.Default.Checklist, 9),
            SmartSuggestion("🌙 Tomorrow prep", "What's scheduled for tomorrow?", Icons.Default.CalendarMonth, 8)
        )
        
        private val NIGHT_SUGGESTIONS = listOf(
            SmartSuggestion("⏰ Morning alarm", "Set alarm for ", Icons.Default.Alarm, 10),
            SmartSuggestion("📝 Quick note", "Remind me tomorrow to ", Icons.Default.Note, 9),
            SmartSuggestion("💤 Sleep reminder", "Remind me at 10 PM to sleep", Icons.Default.Bedtime, 8)
        )
        
        private val OVERDUE_SUGGESTIONS = listOf(
            SmartSuggestion("⚠️ Overdue tasks", "Show overdue tasks", Icons.Default.Warning, 15)
        )
        
        private val UPCOMING_SUGGESTIONS = listOf(
            SmartSuggestion("🔔 Coming up", "What's coming up?", Icons.Default.NotificationsActive, 14)
        )
        
        private val BUDGET_SUGGESTIONS = listOf(
            SmartSuggestion("💸 Budget alert", "Show my budget status", Icons.Default.MoneyOff, 15)
        )
        
        private val DEFAULT_SUGGESTIONS = listOf(
            SmartSuggestion("Set a reminder", "Remind me to ", Icons.Default.Notifications),
            SmartSuggestion("Check schedule", "What's on my schedule?", Icons.Default.Event),
            SmartSuggestion("Track expense", "I spent ", Icons.Default.AttachMoney),
            SmartSuggestion("Create task", "Create task ", Icons.Default.AddTask)
        )
    }
    
    /**
     * Get current time of day
     */
    fun getCurrentTimeOfDay(): TimeOfDay {
        val hour = LocalTime.now().hour
        return when {
            hour in 5..11 -> TimeOfDay.MORNING
            hour in 12..16 -> TimeOfDay.AFTERNOON
            hour in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
    
    /**
     * Generate smart suggestions based on context
     */
    fun getSuggestions(context: SuggestionContext): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // Add high-priority context-based suggestions first
        if (context.hasOverdueTasks) {
            suggestions.addAll(OVERDUE_SUGGESTIONS)
        }
        
        if (context.hasUpcomingReminders) {
            suggestions.addAll(UPCOMING_SUGGESTIONS)
        }
        
        if (context.hasPendingBudgetAlerts) {
            suggestions.addAll(BUDGET_SUGGESTIONS)
        }
        
        // Add follow-up suggestions based on last action
        context.lastAction?.let { action ->
            suggestions.addAll(getFollowUpSuggestions(action))
        }
        
        // Add time-based suggestions
        val timeSuggestions = when (context.timeOfDay) {
            TimeOfDay.MORNING -> MORNING_SUGGESTIONS
            TimeOfDay.AFTERNOON -> AFTERNOON_SUGGESTIONS
            TimeOfDay.EVENING -> EVENING_SUGGESTIONS
            TimeOfDay.NIGHT -> NIGHT_SUGGESTIONS
        }
        suggestions.addAll(timeSuggestions)
        
        // Sort by priority and take top 4
        return suggestions
            .distinctBy { it.command }
            .sortedByDescending { it.priority }
            .take(4)
    }
    
    /**
     * Get follow-up suggestions based on last action
     */
    private fun getFollowUpSuggestions(action: LastAction): List<SmartSuggestion> {
        return when (action) {
            is LastAction.CreatedReminder -> listOf(
                SmartSuggestion("➕ Another reminder", "Remind me to ", Icons.Default.Add, 12),
                SmartSuggestion("📋 View reminders", "Show all reminders", Icons.Default.List, 11)
            )
            is LastAction.CreatedTask -> listOf(
                SmartSuggestion("➕ Another task", "Create task ", Icons.Default.Add, 12),
                SmartSuggestion("📋 View tasks", "Show my tasks", Icons.Default.List, 11)
            )
            is LastAction.SetAlarm -> listOf(
                SmartSuggestion("⏰ Another alarm", "Set alarm at ", Icons.Default.Alarm, 12),
                SmartSuggestion("📋 View alarms", "Show my alarms", Icons.Default.AlarmOn, 11)
            )
            is LastAction.AddedTransaction -> listOf(
                SmartSuggestion("💰 Add another", "I spent ", Icons.Default.Add, 12),
                SmartSuggestion("📊 View summary", "Show spending summary", Icons.Default.PieChart, 11)
            )
            is LastAction.ViewedSchedule -> listOf(
                SmartSuggestion("➕ Add event", "Remind me to ", Icons.Default.Add, 12),
                SmartSuggestion("📅 Tomorrow", "What's tomorrow?", Icons.Default.CalendarMonth, 11)
            )
            is LastAction.ViewedFinance -> listOf(
                SmartSuggestion("💰 Add expense", "I spent ", Icons.Default.Add, 12),
                SmartSuggestion("💵 Add income", "I received ", Icons.Default.AttachMoney, 11)
            )
        }
    }
    
    /**
     * Get default suggestions when no context is available
     */
    fun getDefaultSuggestions(): List<SmartSuggestion> {
        return DEFAULT_SUGGESTIONS
    }
}
