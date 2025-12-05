package com.example.voicereminder.domain

import android.content.Context
import android.util.Log
import com.example.voicereminder.data.RecurrenceRule
import com.example.voicereminder.data.RecurrenceType
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.domain.models.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Scheduler for handling recurring reminders and tasks
 */
class RecurrenceScheduler(
    private val database: ReminderDatabase,
    private val context: Context
) {
    companion object {
        private const val TAG = "RecurrenceScheduler"
        
        @Volatile
        private var INSTANCE: RecurrenceScheduler? = null
        
        fun getInstance(context: Context): RecurrenceScheduler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecurrenceScheduler(
                    ReminderDatabase.getDatabase(context),
                    context.applicationContext
                ).also { INSTANCE = it }
            }
        }
    }
    
    private val reminderDao = database.reminderDao()
    private val notificationScheduler = NotificationScheduler(context)
    
    /**
     * Calculate the next occurrence based on the recurrence rule
     * 
     * @param currentTime The current or last occurrence time
     * @param rule The recurrence rule
     * @return The next occurrence time, or null if no more occurrences
     */
    fun calculateNextOccurrence(
        currentTime: LocalDateTime,
        rule: RecurrenceRule
    ): LocalDateTime? {
        if (!rule.isRecurring()) return null
        
        // Check if we've exceeded the end date
        if (rule.endDate != null) {
            val endDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(rule.endDate),
                ZoneId.systemDefault()
            )
            if (currentTime.isAfter(endDateTime)) {
                return null
            }
        }
        
        val nextTime = when (rule.type) {
            RecurrenceType.DAILY -> calculateNextDaily(currentTime, rule.interval)
            RecurrenceType.WEEKLY -> calculateNextWeekly(currentTime, rule.interval, rule.daysOfWeek)
            RecurrenceType.MONTHLY -> calculateNextMonthly(currentTime, rule.interval, rule.dayOfMonth)
            RecurrenceType.YEARLY -> calculateNextYearly(currentTime, rule.interval)
            RecurrenceType.CUSTOM -> calculateNextCustom(currentTime, rule)
            RecurrenceType.NONE -> null
        }
        
        // Verify the next time doesn't exceed end date
        if (nextTime != null && rule.endDate != null) {
            val endDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(rule.endDate),
                ZoneId.systemDefault()
            )
            if (nextTime.isAfter(endDateTime)) {
                return null
            }
        }
        
        return nextTime
    }
    
    /**
     * Get all occurrences within a date range (for calendar display)
     */
    fun getOccurrencesInRange(
        rule: RecurrenceRule,
        startTime: LocalDateTime,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDateTime> {
        if (!rule.isRecurring()) return emptyList()
        
        val occurrences = mutableListOf<LocalDateTime>()
        var current = startTime
        val endDateTime = endDate.plusDays(1).atStartOfDay()
        val startDateTime = startDate.atStartOfDay()
        
        // Limit iterations to prevent infinite loops
        var iterations = 0
        val maxIterations = 366 // Max one year of daily occurrences
        
        while (current.isBefore(endDateTime) && iterations < maxIterations) {
            if (!current.isBefore(startDateTime)) {
                occurrences.add(current)
            }
            
            val next = calculateNextOccurrence(current, rule)
            if (next == null || next.isEqual(current)) break
            current = next
            iterations++
        }
        
        return occurrences
    }
    
    /**
     * Schedule the next occurrence of a recurring reminder
     */
    suspend fun scheduleNextRecurrence(reminderId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val reminderEntity = reminderDao.getReminderById(reminderId)
                if (reminderEntity == null) {
                    Log.e(TAG, "Reminder not found: $reminderId")
                    return@withContext
                }
                
                val rule = RecurrenceRule.fromJson(reminderEntity.recurrenceRule)
                if (rule == null || !rule.isRecurring()) {
                    Log.d(TAG, "Reminder $reminderId is not recurring")
                    return@withContext
                }
                
                val currentTime = reminderEntity.scheduledTime?.let {
                    LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault())
                } ?: LocalDateTime.now()
                
                val nextTime = calculateNextOccurrence(currentTime, rule)
                if (nextTime == null) {
                    Log.d(TAG, "No more occurrences for reminder $reminderId")
                    // Mark as completed since recurrence has ended
                    val updatedEntity = reminderEntity.copy(status = ReminderStatus.COMPLETED)
                    reminderDao.update(updatedEntity)
                    return@withContext
                }
                
                // Update the reminder with the next scheduled time
                val nextTimeMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val updatedEntity = reminderEntity.copy(
                    scheduledTime = nextTimeMillis,
                    status = ReminderStatus.PENDING,
                    snoozeCount = 0,
                    originalScheduledTime = null
                )
                reminderDao.update(updatedEntity)
                
                // Schedule the notification
                val reminder = updatedEntity.toDomain()
                notificationScheduler.scheduleReminder(reminder)
                
                Log.d(TAG, "Next occurrence scheduled for reminder $reminderId at $nextTime")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling next recurrence", e)
            }
        }
    }
    
    /**
     * Calculate next daily occurrence
     */
    private fun calculateNextDaily(current: LocalDateTime, interval: Int): LocalDateTime {
        return current.plusDays(interval.toLong())
    }
    
    /**
     * Calculate next weekly occurrence
     */
    private fun calculateNextWeekly(
        current: LocalDateTime,
        interval: Int,
        daysOfWeek: List<Int>?
    ): LocalDateTime {
        if (daysOfWeek.isNullOrEmpty()) {
            // Simple weekly recurrence
            return current.plusWeeks(interval.toLong())
        }
        
        // Find the next valid day of week
        var next = current.plusDays(1)
        var weeksAdded = 0
        val sortedDays = daysOfWeek.sorted()
        
        // Look for the next valid day within the current week first
        while (next.dayOfWeek.value !in sortedDays) {
            next = next.plusDays(1)
            
            // If we've gone past Sunday, move to the next interval week
            if (next.dayOfWeek == DayOfWeek.MONDAY && weeksAdded == 0) {
                if (interval > 1) {
                    next = next.plusWeeks((interval - 1).toLong())
                }
                weeksAdded = 1
            }
        }
        
        return next
    }
    
    /**
     * Calculate next monthly occurrence
     */
    private fun calculateNextMonthly(
        current: LocalDateTime,
        interval: Int,
        dayOfMonth: Int?
    ): LocalDateTime {
        var next = current.plusMonths(interval.toLong())
        
        if (dayOfMonth != null) {
            // Adjust to the specified day of month
            val maxDay = next.toLocalDate().lengthOfMonth()
            val targetDay = minOf(dayOfMonth, maxDay)
            next = next.withDayOfMonth(targetDay)
            
            // If the target day is before or equal to current, move to next month
            if (!next.isAfter(current)) {
                next = next.plusMonths(interval.toLong())
                val newMaxDay = next.toLocalDate().lengthOfMonth()
                next = next.withDayOfMonth(minOf(dayOfMonth, newMaxDay))
            }
        }
        
        return next
    }
    
    /**
     * Calculate next yearly occurrence
     */
    private fun calculateNextYearly(current: LocalDateTime, interval: Int): LocalDateTime {
        var next = current.plusYears(interval.toLong())
        
        // Handle leap year edge case (Feb 29)
        if (current.monthValue == 2 && current.dayOfMonth == 29) {
            if (!next.toLocalDate().isLeapYear) {
                next = next.withDayOfMonth(28)
            }
        }
        
        return next
    }
    
    /**
     * Calculate next custom occurrence (combines multiple rules)
     */
    private fun calculateNextCustom(current: LocalDateTime, rule: RecurrenceRule): LocalDateTime {
        // For custom rules, use the most specific pattern available
        return when {
            rule.daysOfWeek != null -> calculateNextWeekly(current, rule.interval, rule.daysOfWeek)
            rule.dayOfMonth != null -> calculateNextMonthly(current, rule.interval, rule.dayOfMonth)
            else -> calculateNextDaily(current, rule.interval)
        }
    }
    
    /**
     * Check if a date matches the recurrence rule
     */
    fun matchesDate(rule: RecurrenceRule, date: LocalDate, startDate: LocalDate): Boolean {
        if (!rule.isRecurring()) return false
        
        return when (rule.type) {
            RecurrenceType.DAILY -> {
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysBetween >= 0 && daysBetween % rule.interval == 0L
            }
            RecurrenceType.WEEKLY -> {
                val dayOfWeek = date.dayOfWeek.value
                rule.daysOfWeek?.contains(dayOfWeek) == true
            }
            RecurrenceType.MONTHLY -> {
                rule.dayOfMonth == date.dayOfMonth ||
                    (rule.dayOfMonth != null && rule.dayOfMonth > date.lengthOfMonth() && 
                     date.dayOfMonth == date.lengthOfMonth())
            }
            RecurrenceType.YEARLY -> {
                date.monthValue == startDate.monthValue && date.dayOfMonth == startDate.dayOfMonth
            }
            else -> false
        }
    }
}
