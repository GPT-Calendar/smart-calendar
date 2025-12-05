package com.example.voicereminder.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an alarm in the database
 */
@Entity(
    tableName = "alarms",
    indices = [
        Index(value = ["isEnabled"]),
        Index(value = ["nextTrigger"])
    ]
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val label: String = "Alarm",
    
    val hour: Int, // 0-23 (24-hour format)
    
    val minute: Int, // 0-59
    
    val isEnabled: Boolean = true,
    
    /**
     * Comma-separated days of week for repeat
     * Empty string = one-time alarm
     * "1,2,3,4,5" = weekdays (Mon-Fri)
     * "6,7" = weekends (Sat-Sun)
     * "1,2,3,4,5,6,7" = every day
     */
    val repeatDays: String = "",
    
    val soundUri: String? = null, // Custom alarm sound URI
    
    val vibrate: Boolean = true,
    
    val snoozeCount: Int = 0, // Number of times snoozed
    
    val snoozeDurationMinutes: Int = 5, // Default snooze duration
    
    val lastTriggered: Long? = null, // Unix timestamp of last trigger
    
    val nextTrigger: Long? = null, // Calculated next trigger time
    
    val createdAt: Long // Unix timestamp in milliseconds
) {
    /**
     * Get repeat days as a list of integers (1=Monday, 7=Sunday)
     */
    fun getRepeatDaysList(): List<Int> {
        return if (repeatDays.isBlank()) {
            emptyList()
        } else {
            repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
    }
    
    /**
     * Check if this is a repeating alarm
     */
    fun isRepeating(): Boolean = repeatDays.isNotBlank()
    
    /**
     * Check if alarm repeats on a specific day
     */
    fun repeatsOn(dayOfWeek: Int): Boolean {
        return getRepeatDaysList().contains(dayOfWeek)
    }
    
    /**
     * Get formatted time string (e.g., "7:30 AM")
     */
    fun getFormattedTime(): String {
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }
    
    /**
     * Get repeat days description
     */
    fun getRepeatDescription(): String {
        val days = getRepeatDaysList()
        return when {
            days.isEmpty() -> "One time"
            days == listOf(1, 2, 3, 4, 5) -> "Weekdays"
            days == listOf(6, 7) -> "Weekends"
            days == listOf(1, 2, 3, 4, 5, 6, 7) -> "Every day"
            else -> days.map { getDayAbbreviation(it) }.joinToString(", ")
        }
    }
    
    private fun getDayAbbreviation(day: Int): String {
        return when (day) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> ""
        }
    }
    
    companion object {
        /**
         * Create repeat days string from list
         */
        fun createRepeatDays(days: List<Int>): String {
            return days.sorted().joinToString(",")
        }
        
        /**
         * Create weekdays repeat string
         */
        fun weekdays(): String = "1,2,3,4,5"
        
        /**
         * Create weekends repeat string
         */
        fun weekends(): String = "6,7"
        
        /**
         * Create every day repeat string
         */
        fun everyDay(): String = "1,2,3,4,5,6,7"
    }
}
