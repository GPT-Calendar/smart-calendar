package com.example.voicereminder.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Types of recurrence patterns
 */
enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

/**
 * Data class representing a recurrence rule for reminders, tasks, and alarms
 */
data class RecurrenceRule(
    @SerializedName("type")
    val type: RecurrenceType = RecurrenceType.NONE,
    
    @SerializedName("interval")
    val interval: Int = 1, // Every X days/weeks/months
    
    @SerializedName("daysOfWeek")
    val daysOfWeek: List<Int>? = null, // 1=Monday, 7=Sunday (ISO-8601)
    
    @SerializedName("dayOfMonth")
    val dayOfMonth: Int? = null, // 1-31 for monthly recurrence
    
    @SerializedName("endDate")
    val endDate: Long? = null, // Unix timestamp for end date
    
    @SerializedName("maxOccurrences")
    val maxOccurrences: Int? = null // Maximum number of occurrences
) {
    companion object {
        private val gson = Gson()
        
        /**
         * Create a daily recurrence rule
         */
        fun daily(interval: Int = 1, endDate: Long? = null): RecurrenceRule {
            return RecurrenceRule(
                type = RecurrenceType.DAILY,
                interval = interval,
                endDate = endDate
            )
        }
        
        /**
         * Create a weekly recurrence rule
         * @param daysOfWeek List of days (1=Monday, 7=Sunday)
         */
        fun weekly(daysOfWeek: List<Int>, interval: Int = 1, endDate: Long? = null): RecurrenceRule {
            return RecurrenceRule(
                type = RecurrenceType.WEEKLY,
                interval = interval,
                daysOfWeek = daysOfWeek,
                endDate = endDate
            )
        }
        
        /**
         * Create a weekdays-only recurrence rule (Monday-Friday)
         */
        fun weekdays(endDate: Long? = null): RecurrenceRule {
            return RecurrenceRule(
                type = RecurrenceType.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(1, 2, 3, 4, 5), // Mon-Fri
                endDate = endDate
            )
        }
        
        /**
         * Create a weekends-only recurrence rule (Saturday-Sunday)
         */
        fun weekends(endDate: Long? = null): RecurrenceRule {
            return RecurrenceRule(
                type = RecurrenceType.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(6, 7), // Sat-Sun
                endDate = endDate
            )
        }
        
        /**
         * Create a monthly recurrence rule
         * @param dayOfMonth Day of month (1-31)
         */
        fun monthly(dayOfMonth: Int, interval: Int = 1, endDate: Long? = null): RecurrenceRule {
            return RecurrenceRule(
                type = RecurrenceType.MONTHLY,
                interval = interval,
                dayOfMonth = dayOfMonth,
                endDate = endDate
            )
        }
        
        /**
         * Create a yearly recurrence rule
         */
        fun yearly(interval: Int = 1, endDate: Long? = null): RecurrenceRule {
            return RecurrenceRule(
                type = RecurrenceType.YEARLY,
                interval = interval,
                endDate = endDate
            )
        }
        
        /**
         * Parse RecurrenceRule from JSON string
         */
        fun fromJson(json: String?): RecurrenceRule? {
            if (json.isNullOrBlank()) return null
            return try {
                gson.fromJson(json, RecurrenceRule::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert to JSON string for database storage
     */
    fun toJson(): String {
        return gson.toJson(this)
    }
    
    /**
     * Check if this rule has any recurrence (not NONE)
     */
    fun isRecurring(): Boolean = type != RecurrenceType.NONE
    
    /**
     * Get a human-readable description of the recurrence
     */
    fun getDescription(): String {
        return when (type) {
            RecurrenceType.NONE -> "Does not repeat"
            RecurrenceType.DAILY -> {
                if (interval == 1) "Daily"
                else "Every $interval days"
            }
            RecurrenceType.WEEKLY -> {
                val daysStr = daysOfWeek?.let { days ->
                    if (days == listOf(1, 2, 3, 4, 5)) "Weekdays"
                    else if (days == listOf(6, 7)) "Weekends"
                    else days.map { getDayName(it) }.joinToString(", ")
                } ?: "Weekly"
                
                if (interval == 1) daysStr
                else "Every $interval weeks on $daysStr"
            }
            RecurrenceType.MONTHLY -> {
                val dayStr = dayOfMonth?.let { "on day $it" } ?: ""
                if (interval == 1) "Monthly $dayStr"
                else "Every $interval months $dayStr"
            }
            RecurrenceType.YEARLY -> {
                if (interval == 1) "Yearly"
                else "Every $interval years"
            }
            RecurrenceType.CUSTOM -> "Custom"
        }
    }
    
    private fun getDayName(day: Int): String {
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
}
